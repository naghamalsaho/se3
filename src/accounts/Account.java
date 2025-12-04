package accounts;

import notifications.NotificationObserver;
import java.util.ArrayList;
import java.util.List;

public interface Account {
    String getId();
    String getName();
    double getBalance();
    void deposit(double amount);
    void withdraw(double amount) throws IllegalStateException;
    void addObserver(NotificationObserver observer);
    void removeObserver(NotificationObserver observer);
    void notifyObservers(String event, String message);
}
