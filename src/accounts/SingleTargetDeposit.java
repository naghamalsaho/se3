package accounts;

import java.util.*;

public class SingleTargetDeposit implements DepositStrategy {
    private final Account target;
    public SingleTargetDeposit(Account target){ this.target = target; }
    @Override
    public Map<Account, Double> splitDeposit(List<Account> children, double amount) {
        Map<Account, Double> plan = new LinkedHashMap<>();
        // deposit all to single target (must be child)
        if (children.contains(target)) plan.put(target, amount);
        else throw new IllegalArgumentException("Target not a child of group");
        return plan;
    }
}
