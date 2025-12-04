package interest;

import accounts.Account;
import accounts.SavingsAccount;

public class CompoundInterestStrategy implements InterestStrategy {
    private final double yearlyRate;
    private final int compoundsPerYear;
    public CompoundInterestStrategy(double yearlyRate, int compoundsPerYear){
        this.yearlyRate = yearlyRate; this.compoundsPerYear = compoundsPerYear;
    }



    @Override
    public double computeInterest(SavingsAccount account, int months) {
        double principal = account.getBalance();
        double n = compoundsPerYear;
        double t = months / 12.0;
        return principal * (Math.pow(1 + yearlyRate/100.0 / n, n*t) - 1);
    }
}
