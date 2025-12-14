package test.java.groups;


import accounts.Account;
import accounts.AccountGroup;
import accounts.factory.AccountFactory;
import accounts.EvenSplitDeposit;
import accounts.SequentialWithdraw;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountGroupStrategyTest {
    @Test
    void evenSplitDepositDistributesSum() {
        List<Account> children = List.of(
                AccountFactory.createSavings("a1", "a1", 0.0),
                AccountFactory.createSavings("a2", "a2", 0.0),
                AccountFactory.createSavings("a3", "a3", 0.0)
        );

        EvenSplitDeposit strategy = new EvenSplitDeposit();
        Map<Account, Double> plan = strategy.splitDeposit(children, 100.0);

        double sum = plan.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, sum, 0.001);
    }

    @Test
    void sequentialWithdrawFailsIfNotEnough() {
        Account a1 = AccountFactory.createSavings("a1","a1", 10.0);
        Account a2 = AccountFactory.createSavings("a2","a2", 5.0);
        List<Account> children = List.of(a1, a2);

        SequentialWithdraw strategy = new SequentialWithdraw();
        assertThrows(IllegalStateException.class, () -> strategy.splitWithdraw(children, 30.0));
    }
}
