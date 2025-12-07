package accounts;

import java.util.List;
import java.util.Map;

public interface WithdrawStrategy {
    // returns map child -> amount to withdraw (throws if not possible)
    Map<Account, Double> splitWithdraw(List<Account> children, double amount) throws IllegalStateException;
}
