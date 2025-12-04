package interest;

import accounts.SavingsAccount;
import accounts.Account;

public interface InterestStrategy {
    double computeInterest(SavingsAccount account, int months);
}
