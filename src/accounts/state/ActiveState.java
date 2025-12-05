package accounts.state;

import accounts.Account;

public class ActiveState implements AccountStatus {

    @Override
    public void deposit(Account account, double amount) {
        account.depositInternal(amount);
    }

    @Override
    public void withdraw(Account account, double amount) {
        account.withdrawInternal(amount);
    }

    @Override
    public String name() {
        return "ACTIVE";
    }
}
