package banking_system;

import accounts.Account;
import payment.PaymentService;
import transactions.Transaction;
import transactions.TransactionService;
import security.AuthService;
import security.Role;
import transactions.AuditLog;

import java.util.List;

public class BankingFacade {
    private final TransactionService txService;
    private final AuthService auth;
    private final PaymentService paymentService;

    public BankingFacade(TransactionService txService, AuthService auth, PaymentService paymentService) {
        this.txService = txService;
        this.auth = auth;
        this.paymentService = paymentService;
    }

    /**
     * Simplified transfer that first authorizes caller and then processes the transaction.
     *
     * For "large" transfers (demo threshold 10_000) we treat them as external:
     *  - pre-withdraw from source account (fail if insufficient funds)
     *  - call external payment gateway (adapter)
     *  - if gateway fails, roll back by re-depositing into source account
     *
     * For local/small transfers we delegate to txService.process(...) which handles
     * validation, approval chain, execution and auditing.
     */
    public boolean transfer(String userId, Transaction tx) {
        if (!auth.authorize(userId, Role.CUSTOMER)) {
            System.out.println("[Facade] Unauthorized");
            return false;
        }

        // Demo rule: treat transfers over 10_000 as external payments (use adapter)
        if (tx.getAmount() > 10_000) {
            // must have a source account for external transfers in this model
            if (tx.getFrom() == null) {
                System.out.println("[Facade] External transfer requires a source account");
                txService.getAuditLog().record(tx, "REJECTED: no source account for external transfer");
                return false;
            }

            // Attempt to pre-withdraw from source account (ensures funds reserved)
            try {
                tx.getFrom().withdraw(tx.getAmount());
            } catch (Exception e) {
                // withdrawal failed (insufficient funds or state); record and return
                System.out.println("[Facade] External transfer aborted: " + e.getMessage());
                txService.getAuditLog().record(tx, "REJECTED: " + e.getMessage());
                return false;
            }

            // Call external payment gateway
            boolean externalOk;
            try {
                externalOk = paymentService.processExternalTransfer(tx);
            } catch (Throwable t) {
                externalOk = false;
            }

            if (externalOk) {
                txService.getAuditLog().record(tx, "EXTERNAL_EXECUTED");
                return true;
            } else {
                // rollback the reserved funds
                try {
                    tx.getFrom().deposit(tx.getAmount());
                } catch (Exception depositEx) {
                    // This would be very unusual; at minimum log
                    System.out.println("[Facade] Failed to rollback funds after external failure: " + depositEx.getMessage());
                }
                txService.getAuditLog().record(tx, "EXTERNAL_FAILED_ROLLED_BACK");
                return false;
            }
        } else {
            // Local/small transfer - let TransactionService handle validation, approval and execution
            return txService.process(tx);
        }
    }

    public boolean deposit(String userId, Transaction tx) {
        if (!auth.authorize(userId, Role.CUSTOMER)) {
            System.out.println("[Facade] Unauthorized deposit");
            return false;
        }
        return txService.process(tx);
    }

    public void scheduleRecurring(String userId, transactions.RecurringTransaction rtx, long initialDelay, long period) {
        if (!auth.authorize(userId, Role.CUSTOMER)) {
            System.out.println("[Facade] Unauthorized schedule");
            return;
        }
        txService.scheduleRecurring(rtx, initialDelay, period);
    }

    public List<Transaction> history() {
        return txService.getHistory();
    }

    public AuditLog audit() {
        return txService.getAuditLog();
    }
}
