package test.java.transactions;

import accounts.Account;
import accounts.factory.AccountFactory;
import org.junit.jupiter.api.Test;
import transactions.Transaction;
import transactions.TransactionHandler;
import transactions.TransactionService;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class TransactionServiceTest {
    @Test
    void processDepositRecordsHistoryAndAudit() {
        // mock the chain to always approve
        TransactionHandler chain = mock(TransactionHandler.class);
        when(chain.handle(any())).thenReturn(true);

        TransactionService svc = new TransactionService(chain);
        Account to = AccountFactory.createSavings(null, "t", 0.0);
        Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, to, 100.0);

        boolean ok = svc.process(tx);

        assertTrue(ok);
        assertEquals(1, svc.getHistory().size(), "history should contain the executed tx");
        assertEquals(1, svc.getAuditLog().entriesCount(), "audit should have one entry");
    }
}
