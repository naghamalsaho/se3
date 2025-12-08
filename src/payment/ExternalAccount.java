package payment;

import accounts.Account;
import accounts.state.AccountStatus;
import accounts.state.ActiveState;
import notifications.NotificationObserver;

import java.util.ArrayList;
import java.util.List;

public class ExternalAccount implements Account {
    private final String id;
    private final String name;
    private double balance = 0.0;
    private final List<NotificationObserver> observers = new ArrayList<>();
    private AccountStatus status = new ActiveState();

    // <-- here: make constructor PUBLIC so other packages can call it
    public ExternalAccount(String id, String name) {
        this.id = id == null ? "external" : id;
        this.name = name == null ? "external" : name;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public double getBalance() { return balance; }

    @Override public void deposit(double amount) { depositInternal(amount); }
    @Override public void withdraw(double amount) { withdrawInternal(amount); }

    @Override public void depositInternal(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount>0");
        double old = balance;
        balance += amount;
        notifyObservers("deposit", String.format("Deposit %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override public void withdrawInternal(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount>0");
        double old = balance;
        balance -= amount;
        notifyObservers("withdraw", String.format("Withdraw %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override public void addObserver(NotificationObserver observer) { if (!observers.contains(observer)) observers.add(observer); }
    @Override public void removeObserver(NotificationObserver observer) { observers.remove(observer); }
    @Override public void notifyObservers(String event, String message) {
        for (var o : new ArrayList<>(observers)) { o.update(this, event, message); }
    }

    @Override public AccountStatus getStatus() { return status; }
    @Override public void setStatus(AccountStatus status) { this.status = status; }
    @Override public String getStatusName() { return status.name(); }
    @Override public void freeze() { setStatus(status); }
    @Override public void suspend() { setStatus(status); }
    @Override public void close() { setStatus(status); }
    @Override public void reopen() { setStatus(new ActiveState()); }
}
