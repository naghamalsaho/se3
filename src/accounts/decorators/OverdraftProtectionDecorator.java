package accounts.decorators;

import accounts.Account;
import accounts.CheckingAccount;
import notifications.NotificationObserver;

public class OverdraftProtectionDecorator extends AccountDecorator {

    private final double extraLimit; // positive amount to add

    public OverdraftProtectionDecorator(Account wrapped, double extraLimit) {
        super(wrapped);
        this.extraLimit = Math.max(0.0, extraLimit);

        // If it's a CheckingAccount, increase its overdraft limit
        if (wrapped instanceof CheckingAccount) {
            CheckingAccount chk = (CheckingAccount) wrapped;
            double current = chk.getOverdraftLimit();
            chk.setOverdraftLimit(current + this.extraLimit);
        } else {
            System.out.println("[Overdraft] Not applied: wrapped account is not CheckingAccount");
        }
    }

    @Override
    public void withdraw(double amount) {
        try {
            super.withdraw(amount);
        } catch (RuntimeException ex) {
            this.notifyObservers("overdraft_failed", "Overdraft protection failed: " + ex.getMessage());
            throw ex;
        }
    }
    @Override
    public double getAvailableBalance() {
        double base = wrapped.getAvailableBalance();
        // extraLimit here is the extra positive amount we allow (e.g., 500)
        // if you used CheckingAccount.setOverdraftLimit(-abs) then you can compute differently.
        return base + Math.abs(extraLimit);
    }
}
