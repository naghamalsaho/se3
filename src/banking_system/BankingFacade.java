package banking_system;

import accounts.Account;
import payment.PaymentService;
import transactions.Transaction;
import transactions.TransactionService;
import security.AuthService;
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

    // simplified transfer that first authorizes caller and then processes tx
    public boolean transfer(String userId, Transaction tx) {
        if (!auth.authorize(userId, security.Role.CUSTOMER)) {
            System.out.println("[Facade] Unauthorized");
            return false;
        }
        // if external transfer flag set (could add property in Transaction) – use paymentService
        // For demo, assume transfers over 10000 use external gateway
        if (tx.getAmount() > 10000) {
            boolean ok = paymentService.processExternalTransfer(tx);
            if (ok) {
                txService.getAuditLog().record(tx, "EXTERNAL_EXECUTED");
            }
            return ok;
        } else {
            return txService.process(tx);
        }
    }

    public boolean deposit(String userId, Transaction tx) {
        if (!auth.authorize(userId, security.Role.CUSTOMER)) {
            System.out.println("[Facade] Unauthorized deposit");
            return false;
        }
        return txService.process(tx);
    }

    public void scheduleRecurring(String userId, transactions.RecurringTransaction rtx, long initialDelay, long period) {
        if (!auth.authorize(userId, security.Role.CUSTOMER)) {
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
