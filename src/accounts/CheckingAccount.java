package accounts;

import accounts.state.*;
import notifications.NotificationObserver;

import java.util.ArrayList;
import java.util.List;

public class CheckingAccount implements Account {

    private final String id;
    private final String name;
    private double balance;
    private final List<NotificationObserver> observers = new ArrayList<>();

    // overdraft allowed (positive number) — default 500
    private double overdraftLimit = 500.0;

    private AccountStatus status = new ActiveState();

    public CheckingAccount(String id, String name, double initial) {
        this.id = id;
        this.name = name;
        this.balance = initial;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public double getBalance() { return balance; }

    // ------------------------ PUBLIC OPS ---------------------------- //

    @Override
    public void deposit(double amount) {
        status.deposit(this, amount);
    }

    @Override
    public void withdraw(double amount) {
        status.withdraw(this, amount);
    }

    // ------------------------ INTERNAL OPS --------------------------- //

    @Override
    public void depositInternal(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount>0");

        double old = balance;
        balance += amount;

        notifyObservers("deposit",
                String.format("Deposit %.2f (old: %.2f -> new: %.2f)",
                        amount, old, balance));
    }

    @Override
    public void withdrawInternal(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount>0");

        // compute available = balance + overdraftLimit
        double available = getAvailableBalance();
        if (amount > available) {
            throw new IllegalStateException("Overdraft limit exceeded");
        }

        double old = balance;
        balance -= amount;

        notifyObservers("withdraw",
                String.format("Withdraw %.2f (old: %.2f -> new: %.2f)",
                        amount, old, balance));
    }

    // ------------------------ OBSERVERS ------------------------------ //

    @Override
    public void addObserver(NotificationObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    @Override
    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event, String message) {
        for (NotificationObserver o : observers) {
            o.update(this, event, message);
        }
    }

    // ------------------------ STATE MGMT ----------------------------- //

    @Override
    public AccountStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public String getStatusName() {
        return status.name();
    }

    @Override
    public void freeze() {
        setStatus(new FrozenState());
    }

    @Override
    public void suspend() {
        setStatus(new SuspendedState());
    }

    @Override
    public void close() {
        setStatus(new ClosedState());
    }

    @Override
    public void reopen() {
        setStatus(new ActiveState());
    }

    // ------------------------ OVERDRAFT API -------------------------- //

    public void setOverdraftLimit(double limit) {
        // accept only non-negative numbers (0 = no overdraft)
        this.overdraftLimit = Math.max(0.0, limit);
    }

    public double getOverdraftLimit() {
        return this.overdraftLimit;
    }

    @Override
    public double getAvailableBalance() {
        return getBalance() + getOverdraftLimit();
    }
}
