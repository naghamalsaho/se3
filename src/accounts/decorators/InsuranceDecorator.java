package accounts.decorators;

import accounts.Account;

public class InsuranceDecorator extends AccountDecorator {
    private final double coverAmount; // how much insurance can cover per claim

    public InsuranceDecorator(Account wrapped, double coverAmount) {
        super(wrapped);
        this.coverAmount = coverAmount;
    }

    @Override
    public void withdraw(double amount) {
        try {
            super.withdraw(amount);
        } catch (RuntimeException ex) {
            // If withdrawal failed due to insufficient funds, try to cover by insurance
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("insufficient")) {
                double toCover = Math.min(coverAmount, amount);
                // simulate coverage by depositing money to wrapped account then retry
                wrapped.deposit(toCover);
                notifyObservers("insurance_cover", String.format("Insurance covered %.2f for account %s", toCover, wrapped.getId()));
                // retry withdrawal
                super.withdraw(amount);
            } else {
                throw ex;
            }
        }
    }
}
