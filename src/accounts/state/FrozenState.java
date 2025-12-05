package accounts.state;

import accounts.Account;

public class FrozenState implements AccountStatus {

    @Override
    public void deposit(Account account, double amount) {
        throw new IllegalStateException("Account is frozen. Deposits not allowed.");
    }

    @Override
    public void withdraw(Account account, double amount) {
        throw new IllegalStateException("Account is frozen. Withdrawals not allowed.");
    }

    @Override
    public String name() {
        return "FROZEN";
    }
}
