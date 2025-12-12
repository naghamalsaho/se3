package transactions;

import accounts.Account;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * TransactionService - updated scheduleRecurring behavior:
 * - keep recurring task alive
 * - if insufficient funds or account-state problem: skip actual execution (do NOT call process)
 *   and avoid audit spam by logging/notifying only once per cooldown period per reason.
 * - when funds/state become ok, the normal process(...) is invoked and the task continues.
 */
public class TransactionService {
    private final TransactionHandler approvalChain;
    private final List<Transaction> history = Collections.synchronizedList(new ArrayList<>());
    private final AuditLog auditLog = new AuditLog();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // track last time we logged a "skipped ..." for a recurring tx keyed by rtx+reason
    private final Map<String, Instant> lastFailureLog = new ConcurrentHashMap<>();
    // cooldown to avoid audit/notification spam: e.g. 1 day
    private final Duration failureLogCooldown = Duration.ofHours(24);

    public TransactionService(TransactionHandler approvalChain){
        this.approvalChain = approvalChain;
    }

    public boolean process(Transaction tx){
        // validation + approval happens in chain
        boolean approved = approvalChain.handle(tx);
        if(!approved){
            auditLog.record(tx, "REJECTED");
            return false;
        }
        try{
            // execute
            if(tx.getType() == Transaction.Type.DEPOSIT && tx.getTo() != null){
                tx.getTo().deposit(tx.getAmount());
            } else if(tx.getType() == Transaction.Type.WITHDRAW && tx.getFrom() != null){
                tx.getFrom().withdraw(tx.getAmount());
            } else if(tx.getType() == Transaction.Type.TRANSFER && tx.getFrom() != null && tx.getTo() != null){
                tx.getFrom().withdraw(tx.getAmount());
                tx.getTo().deposit(tx.getAmount());
            }
            history.add(tx);
            auditLog.record(tx, "EXECUTED");
            return true;
        }catch(Exception e){
            auditLog.record(tx, "FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Schedule recurring tx but skip execution while insufficient funds or blocked states.
     * Avoids audit spam: logs a skipped-event only once per cooldown window PER REASON.
     */
    public ScheduledFuture<?> scheduleRecurring(RecurringTransaction rtx, long initialDelaySeconds, long periodSeconds){
        RecurringTask task = new RecurringTask(rtx);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
        task.setFuture(future);
        return future;
    }

    public List<Transaction> getHistory(){ return new ArrayList<>(history); }
    public AuditLog getAuditLog(){ return auditLog; }

    public void shutdown(){ scheduler.shutdown(); }

    /* -----------------------
       Inner recurring task
       ----------------------- */
    private class RecurringTask implements Runnable {
        private final RecurringTransaction rtx;
        private volatile ScheduledFuture<?> future;

        RecurringTask(RecurringTransaction rtx){
            this.rtx = rtx;
        }

        void setFuture(ScheduledFuture<?> f){ this.future = f; }

        private String failureKey(String reason){
            // use identityHashCode for stable per-object key
            return System.identityHashCode(rtx) + "|" + reason;
        }

        private void skipWithCooldown(String reason, String userMessage) {
            Instant now = Instant.now();
            String key = failureKey(reason);
            Instant lastLogged = lastFailureLog.get(key);
            if (lastLogged == null || now.isAfter(lastLogged.plus(failureLogCooldown))) {
                // Log once per cooldown window
                Transaction tx = rtx.toTransaction();
                auditLog.record(tx, "SKIPPED_" + reason);
                // notify owner (observers) once per cooldown
                try {
                    Account from = rtx.getFrom();
                    if (from != null) {
                        from.notifyObservers("recurring", userMessage);
                    }
                } catch (Exception ignored) {}
                lastFailureLog.put(key, now);
            }
        }

        private void clearFailureRecordsForThisRtx() {
            String prefix = String.valueOf(System.identityHashCode(rtx)) + "|";
            for (String k : new ArrayList<>(lastFailureLog.keySet())) {
                if (k.startsWith(prefix)) lastFailureLog.remove(k);
            }
        }

        @Override
        public void run() {
            try {
                Transaction tx = rtx.toTransaction();

                Account from = rtx.getFrom();
                Account to = rtx.getTo();

                // ---------- Check destination status (applies to incoming deposits/transfers) ----------
                if (to != null) {
                    String statusTo = to.getStatusName();
                    if ("CLOSED".equalsIgnoreCase(statusTo)) {
                        // destination closed -> cancel recurring permanently (can't deposit)
                        auditLog.record(tx, "CANCELLED: destination account closed");
                        if (future != null) future.cancel(false);
                        return;
                    }
                    if ("FROZEN".equalsIgnoreCase(statusTo)) {
                        // destination frozen -> skip (notify once per cooldown)
                        skipWithCooldown("DEST_FROZEN", "Recurring payment skipped: destination account frozen.");
                        return;
                    }
                    // SUSPENDED destination: allow incoming deposits/transfers (per SUSPENDED rules)
                }

                // ---------- Pre-checks for TRANSFER/WITHDRAW: source account existence & state & balance ----------
                if (rtx.getType() == Transaction.Type.TRANSFER || rtx.getType() == Transaction.Type.WITHDRAW) {
                    if (from == null) {
                        // malformed recurring transaction; record and cancel permanently
                        auditLog.record(tx, "CANCELLED: no source account");
                        if (future != null) future.cancel(false);
                        return;
                    }

                    String statusFrom = from.getStatusName();
                    if ("CLOSED".equalsIgnoreCase(statusFrom)) {
                        // account closed -> cancel recurring permanently
                        auditLog.record(tx, "CANCELLED: source account closed");
                        if (future != null) future.cancel(false);
                        return;
                    }

                    if ("SUSPENDED".equalsIgnoreCase(statusFrom)) {
                        // Source suspended -> skip (allow deposits to source? depends on policy; here skip outgoing)
                        skipWithCooldown("SRC_SUSPENDED", "Recurring payment skipped: source account suspended.");
                        return; // skip run
                    }

                    if ("FROZEN".equalsIgnoreCase(statusFrom)) {
                        // Source frozen -> skip
                        skipWithCooldown("SRC_FROZEN", "Recurring payment skipped: source account frozen.");
                        return; // skip run
                    }

                    // balance check
                    double available = from.getBalance();
                    double required = rtx.getAmount();
                    if (available < required) {
                        skipWithCooldown("INSUFFICIENT_FUNDS", String.format("Recurring payment of %.2f skipped due to insufficient funds. Available: %.2f", required, available));
                        return;
                    } else {
                        // if sufficient now, clear previous failure records for this recurring tx
                        clearFailureRecordsForThisRtx();
                    }
                }

                // For deposits or when all pre-checks passed: perform the transaction
                boolean ok = process(tx);
                if (ok) {
                    // successful -> clear any failure records so future skips may re-log later if needed
                    clearFailureRecordsForThisRtx();
                } else {
                    // If process returned false, the chain rejected it (other reasons).
                    // Avoid spamming by logging/notify once per cooldown under a general key.
                    skipWithCooldown("REJECTED_BY_CHAIN", "Recurring payment skipped: rejected by validation/approval chain.");
                }
            } catch (Throwable t) {
                // unexpected error; record and continue (do not cancel the scheduled task)
                try {
                    auditLog.record(rtx.toTransaction(), "ERROR in recurring task: " + t.getMessage());
                } catch (Exception ignored){}
            }
        }
    }
}
