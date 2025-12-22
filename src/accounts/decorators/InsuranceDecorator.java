package accounts.decorators;

import accounts.Account;

public class InsuranceDecorator extends AccountDecorator {
    private final double coverAmount; // how much insurance can cover overall
    private double usedCoverage = 0.0; // how much of the cover has been used so far

    public InsuranceDecorator(Account wrapped, double coverAmount) {
        super(wrapped);
        this.coverAmount = coverAmount;
    }

    @Override
    public synchronized void withdraw(double amount) {
        try {
            super.withdraw(amount);
            return;
        } catch (RuntimeException ex) {
            // detect insufficient-funds error (best-effort: message check)
            boolean insufficient = false;
            if (ex.getClass().getSimpleName().toLowerCase().contains("insufficient")) insufficient = true;
            else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("insufficient")) insufficient = true;

            if (!insufficient) {
                throw ex;
            }

            // compute actual deficit = amount - current available balance
            double baseAvailable = wrapped.getAvailableBalance();
            double deficit = amount - baseAvailable;
            if (deficit <= 0) {
                // nothing to cover (some races possible), retry or rethrow
                throw ex;
            }

            double remainingCoverage = Math.max(0.0, coverAmount - usedCoverage);
            double toCover = Math.min(remainingCoverage, deficit);

            if (toCover <= 0) {
                // no coverage left
                throw ex;
            }

            // apply coverage (simulate by depositing) and record usage
            wrapped.deposit(toCover);
            usedCoverage += toCover;

            notifyObservers("insurance_cover",
                    String.format("Insurance covered %.2f for account %s", toCover, wrapped.getId()));

            // retry withdrawal (may still throw if something else fails)
            super.withdraw(amount);
        }
    }

    @Override
    public double getAvailableBalance() {
        double base = wrapped.getAvailableBalance();
        double remainingCoverage = Math.max(0.0, coverAmount - usedCoverage);
        return base + remainingCoverage;
    }
}
