package accounts;

import java.util.List;
import java.util.Map;

public interface DepositStrategy {
    // returns map child -> amount to deposit
    Map<Account, Double> splitDeposit(List<Account> children, double amount);
}
