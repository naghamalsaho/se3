package accounts;

import java.util.*;

public class SequentialWithdraw implements WithdrawStrategy {
    @Override
    public Map<Account, Double> splitWithdraw(List<Account> children, double amount) {
        Map<Account, Double> plan = new LinkedHashMap<>();
        double needed = amount;
        for (Account c : children) {
            double avail = c.getBalance();
            double take = Math.min(avail, needed);
            if (take > 0) {
                plan.put(c, take);
                needed -= take;
            }
            if (needed <= 0) break;
        }
        if (needed > 0) throw new IllegalStateException("Insufficient funds across group. Needed: " + amount + ", available: " + (amount - needed));
        return plan;
    }
}
