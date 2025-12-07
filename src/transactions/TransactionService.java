package transactions;

import accounts.Account;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * TransactionService - updated scheduleRecurring behavior:
 * - keep recurring task alive
 * - if insufficient funds: skip actual execution (do NOT call process)
 *   and avoid audit spam by logging/notifying only once per cooldown period.
 * - when funds become sufficient, the normal process(...) is invoked and the task continues.
 */
public class TransactionService {
    private final TransactionHandler approvalChain;
    private final List<Transaction> history = Collections.synchronizedList(new ArrayList<>());
    private final AuditLog auditLog = new AuditLog();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // track last time we logged a "skipped due to insufficient funds" for a recurring tx
    private final Map<RecurringTransaction, Instant> lastFailureLog = new ConcurrentHashMap<>();
    // cooldown to avoid audit/notification spam: e.g. 1 day
    private final Duration failureLogCooldown = Duration.ofHours(24);

    public TransactionService(TransactionHandler approvalChain){
        this.approvalChain = approvalChain;
    }

    public boolean process(Transaction tx){
        // validation + approval happens in chain (entry point should include validation)
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
     * Schedule recurring tx but skip execution while insufficient funds.
     * Avoids audit spam: logs a skipped-event only once per cooldown window.
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

        @Override
        public void run() {
            try {
                // For transfers/withdraws: pre-check available balance
                if (rtx.getType() == Transaction.Type.TRANSFER || rtx.getType() == Transaction.Type.WITHDRAW) {
                    Account from = rtx.getFrom();
                    if (from == null) {
                        // malformed recurring transaction; record and cancel
                        Transaction tx = rtx.toTransaction();
                        auditLog.record(tx, "CANCELLED: no source account");
                        if (future != null) future.cancel(false);
                        return;
                    }
                    double available = from.getBalance();
                    double required = rtx.getAmount();

                    // If insufficient funds, skip processing (do not call process)
                    if (available < required) {
                        Instant now = Instant.now();
                        Instant lastLogged = lastFailureLog.get(rtx);
                        if (lastLogged == null || now.isAfter(lastLogged.plus(failureLogCooldown))) {
                            // Log once (or once per cooldown) to avoid spam
                            Transaction tx = rtx.toTransaction();
                            auditLog.record(tx, "SKIPPED_INSUFFICIENT_FUNDS");
                            // notify owner (observers) once per cooldown
                            try {
                                from.notifyObservers("recurring", String.format("Recurring payment of %.2f skipped due to insufficient funds. Available: %.2f", required, available));
                            } catch (Exception ignored) {}
                            lastFailureLog.put(rtx, now);
                        }
                        // skip this run (no process call)
                        return;
                    } else {
                        // have sufficient funds now — ensure we clear any previous failure record so future skips will re-log after cooldown
                        if (lastFailureLog.containsKey(rtx)) {
                            lastFailureLog.remove(rtx);
                        }
                    }
                }

                // For deposits or when balance sufficient: perform the transaction
                // قبل التنفيذ، نعمل pre-check للـ approvalChain
                Transaction tx = rtx.toTransaction();
                boolean approved = approvalChain.handle(tx);

                if (!approved) {
                    // سجل مرة واحدة فقط كل cooldown، مثل insufficient funds
                    Instant now = Instant.now();
                    Instant lastLogged = lastFailureLog.get(rtx);

                    if (lastLogged == null || now.isAfter(lastLogged.plus(failureLogCooldown))) {
                        auditLog.record(tx, "SKIPPED_APPROVAL_LIMIT");
                        try {
                            Account from = tx.getFrom();
                            if (from != null) {
                                from.notifyObservers("recurring",
                                        "Recurring payment skipped due to approval limit.");
                            }
                        } catch (Exception ignored) {}
                        lastFailureLog.put(rtx, now);
                    }
                    return; // لا تنفذ العملية
                }

// إذا كل شيء تمام → نفّذها فعليًا
                process(tx);

            } catch (Throwable t) {
                // unexpected error; record and continue (do not cancel the scheduled task)
                try {
                    auditLog.record(rtx.toTransaction(), "ERROR in recurring task: " + t.getMessage());
                } catch (Exception ignored){}
            }
        }
    }
}
