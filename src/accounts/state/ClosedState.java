package accounts.state;

import accounts.Account;

public class ClosedState implements AccountStatus {

    @Override
    public void deposit(Account account, double amount) {
        throw new IllegalStateException("Account is closed. No operations allowed.");
    }

    @Override
    public void withdraw(Account account, double amount) {
        throw new IllegalStateException("Account is closed. No operations allowed.");
    }

    @Override
    public String name() {
        return "CLOSED";
    }
}
