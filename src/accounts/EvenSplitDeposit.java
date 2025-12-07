package accounts;

import java.util.*;

public class EvenSplitDeposit implements DepositStrategy {
    @Override
    public Map<Account, Double> splitDeposit(List<Account> children, double amount) {
        Map<Account, Double> plan = new LinkedHashMap<>();
        if (children.isEmpty()) return plan;
        double per = amount / children.size();
        for (Account c : children) plan.put(c, per);
        return plan;
    }
}
