package accounts.state;

import accounts.Account;

public class FrozenState implements AccountStatus {
    @Override
    public void deposit(Account a, double amount) {
        // Block deposits too (strict freeze)
        throw new IllegalStateException("Account is frozen. Deposits not allowed.");
    }

    @Override
    public void withdraw(Account a, double amount) {
        // Block withdrawals
        throw new IllegalStateException("Account is frozen. Withdrawals not allowed.");
    }

    @Override
    public String name() { return "FROZEN"; }
}
