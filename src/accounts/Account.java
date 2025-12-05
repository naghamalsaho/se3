package accounts;

import accounts.state.AccountStatus;
import notifications.NotificationObserver;

public interface Account {

    String getId();
    String getName();
    double getBalance();

    // Public operations (delegated to state)
    void deposit(double amount);
    void withdraw(double amount);

    // Internal real operations
    void depositInternal(double amount);
    void withdrawInternal(double amount);

    // Observers
    void addObserver(NotificationObserver observer);
    void removeObserver(NotificationObserver observer);
    void notifyObservers(String event, String message);

    // State Pattern
    AccountStatus getStatus();
    void setStatus(AccountStatus status);
    String getStatusName();

    void freeze();
    void suspend();
    void close();
    void reopen();
}
