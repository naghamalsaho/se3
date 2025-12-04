package interest;

import accounts.Account;
import accounts.SavingsAccount;

public class SimpleInterestStrategy implements InterestStrategy {
    private final double yearlyRate;
    public SimpleInterestStrategy(double yearlyRate){ this.yearlyRate = yearlyRate; }



    @Override
    public double computeInterest(SavingsAccount account, int months) {
        double principal = account.getBalance();
        return principal * (yearlyRate/100.0) * (months / 12.0);
    }
}
