package accounts.decorators;

import accounts.Account;
import accounts.CheckingAccount; // we will add setter/getter in CheckingAccount
import notifications.NotificationObserver;

public class OverdraftProtectionDecorator extends AccountDecorator {

    private final double extraLimit;

    public OverdraftProtectionDecorator(Account wrapped, double extraLimit) {
        super(wrapped);
        this.extraLimit = extraLimit;
        // If it's a CheckingAccount, augment its overdraft limit
        if (wrapped instanceof CheckingAccount) {
            CheckingAccount chk = (CheckingAccount) wrapped;
            double current = chk.getOverdraftLimit();
            if (extraLimit > Math.abs(current)) {
                // set a more permissive overdraft (negative)
                chk.setOverdraftLimit(-Math.abs(extraLimit));
            }
        }
    }

    @Override
    public void withdraw(double amount) {
        // delegate: underlying CheckingAccount already supports overdraft; this decorator ensures limit increased
        try {
            super.withdraw(amount);
        } catch (RuntimeException ex) {
            // if still fails, notify observers and rethrow
            this.notifyObservers("overdraft_failed", "Overdraft protection failed: " + ex.getMessage());
            throw ex;
        }
    }
}
