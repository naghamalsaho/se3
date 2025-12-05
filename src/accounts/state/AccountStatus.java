package accounts.state;

import accounts.Account;

public interface AccountStatus {
    void deposit(Account account, double amount);
    void withdraw(Account account, double amount);
    String name();
}
