package accounts.state;

import accounts.Account;

public class SuspendedState implements AccountStatus {

    @Override
    public void deposit(Account account, double amount) {
        throw new IllegalStateException("Account is suspended. Deposits not allowed.");
    }

    @Override
    public void withdraw(Account account, double amount) {
        throw new IllegalStateException("Account is suspended. Withdrawals not allowed.");
    }

    @Override
    public String name() {
        return "SUSPENDED";
    }
}
