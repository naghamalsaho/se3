package accounts.state;

import accounts.Account;

public class SuspendedState implements AccountStatus {

    @Override
    public void deposit(Account account, double amount) {
        // Allow deposits/ incoming transfers so owner can top-up
        account.depositInternal(amount);
    }

    @Override
    public void withdraw(Account account, double amount) {
        // Block withdrawals/outgoing transfers while suspended
        throw new IllegalStateException("Account is suspended. Withdrawals and outgoing transfers are not allowed.");
    }

    @Override
    public String name() {
        return "SUSPENDED";
    }
}
