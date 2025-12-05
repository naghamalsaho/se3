package accounts;

import accounts.state.*;
import notifications.NotificationObserver;
import java.util.ArrayList;
import java.util.List;

public class InvestmentAccount implements Account {
    private final String id;
    private final String name;
    private double balance;
    private String portfolioType;
    private final List<NotificationObserver> observers = new ArrayList<>();
    private AccountStatus status = new ActiveState();

    public InvestmentAccount(String id, String name, double initial, String portfolioType) {
        this.id = id; this.name = name; this.balance = initial; this.portfolioType = portfolioType;
    }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }
    @Override public double getBalance(){ return balance; }

    @Override public void deposit(double amount){ status.deposit(this, amount); }
    @Override public void withdraw(double amount){ status.withdraw(this, amount); }

    @Override public void depositInternal(double amount){
        if(amount <= 0) throw new IllegalArgumentException();
        double old = balance; balance += amount;
        notifyObservers("investment_deposit", String.format("Deposit %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override public void withdrawInternal(double amount){
        if(amount <= 0) throw new IllegalArgumentException();
        if(amount > balance) throw new IllegalStateException("Insufficient funds");
        double old = balance; balance -= amount;
        notifyObservers("investment_withdraw", String.format("Withdraw %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override public void addObserver(NotificationObserver o){ if(!observers.contains(o)) observers.add(o); }
    @Override public void removeObserver(NotificationObserver o){ observers.remove(o); }
    @Override public void notifyObservers(String event, String message){ observers.forEach(obs -> obs.update(this, event, message)); }

    @Override public AccountStatus getStatus(){ return status; }
    @Override public void setStatus(AccountStatus status){ this.status = status; }
    @Override public String getStatusName(){ return status.name(); }
    @Override public void freeze(){ setStatus(new FrozenState()); }
    @Override public void suspend(){ setStatus(new SuspendedState()); }
    @Override public void close(){ setStatus(new ClosedState()); }
    @Override public void reopen(){ setStatus(new ActiveState()); }

    public void setPortfolioType(String t){ this.portfolioType = t; }
    public String getPortfolioType(){ return portfolioType; }
}
