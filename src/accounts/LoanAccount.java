package accounts;

import accounts.state.*;
import notifications.NotificationObserver;
import java.util.ArrayList;
import java.util.List;

public class LoanAccount implements Account {
    private final String id;
    private final String name;
    private double balance; // negative for owed? we treat balance as outstanding principal
    private double interestRate; // yearly
    private final List<NotificationObserver> observers = new ArrayList<>();
    private AccountStatus status = new ActiveState();

    public LoanAccount(String id, String name, double principal, double interestRate) {
        this.id = id; this.name = name; this.balance = principal; this.interestRate = interestRate;
    }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }
    @Override public double getBalance(){ return balance; }

    @Override public void deposit(double amount) { status.deposit(this, amount); } // payment reduces principal
    @Override public void withdraw(double amount) { throw new UnsupportedOperationException("Cannot withdraw from loan"); }

    @Override public void depositInternal(double amount) {
        if(amount <= 0) throw new IllegalArgumentException("Amount>0");
        double old = balance;
        balance -= amount; // pay down
        notifyObservers("loan_payment", String.format("Payment %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override public void withdrawInternal(double amount) { throw new UnsupportedOperationException(); }

    // observers
    @Override public void addObserver(NotificationObserver o){ if(!observers.contains(o)) observers.add(o); }
    @Override public void removeObserver(NotificationObserver o){ observers.remove(o); }
    @Override public void notifyObservers(String event, String message){ observers.forEach(obs -> obs.update(this, event, message)); }

    // status/state
    @Override public AccountStatus getStatus(){ return status; }
    @Override public void setStatus(AccountStatus status){ this.status = status; }
    @Override public String getStatusName(){ return status.name(); }
    @Override public void freeze(){ setStatus(new FrozenState()); }
    @Override public void suspend(){ setStatus(new SuspendedState()); }
    @Override public void close(){ setStatus(new ClosedState()); }
    @Override public void reopen(){ setStatus(new ActiveState()); }

    // modification
    public void setInterestRate(double rate){ this.interestRate = rate; }
    public double getInterestRate(){ return interestRate; }
}
