package accounts.state;

import accounts.Account;

public interface AccountStatus {
    void deposit(Account account, double amount);
    void withdraw(Account account, double amount);
    default boolean canBeSource() { return true; }


    default boolean canReceive() { return true; }

    String name();
}
