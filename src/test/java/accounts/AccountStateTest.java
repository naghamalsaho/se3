package test.java.accounts;

import accounts.Account;
import accounts.factory.AccountFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountStateTest {
    @Test
    void frozenBlocksWithdraw() {
        Account a = AccountFactory.createChecking(null, "u", 100.0);
        a.freeze(); // now frozen
        assertThrows(IllegalStateException.class, () -> a.withdraw(10));
    }

    @Test
    void suspendedBlocksOutgoingWithdrawButAllowsDeposit() {
        Account a = AccountFactory.createChecking(null, "u2", 50.0);
        a.suspend();
        assertThrows(IllegalStateException.class, () -> a.withdraw(10));
        // deposit allowed according to your earlier design: check balance updated
        a.deposit(20);
        assertEquals(70.0, a.getBalance(), 0.001);
    }
}
