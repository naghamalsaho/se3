package accounts;

import notifications.NotificationObserver;
import java.util.ArrayList;
import java.util.List;

public class CheckingAccount implements Account {
    private final String id;
    private final String name;
    private double balance;
    private final List<NotificationObserver> observers = new ArrayList<>();

    // simple overdraft example (allow negative up to -500)
    private double overdraftLimit = -500.0;

    public CheckingAccount(String id, String name, double initial) {
        this.id = id; this.name = name; this.balance = initial;
    }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }
    @Override public double getBalance(){ return balance; }

    @Override
    public void deposit(double amount) {
        if(amount <= 0) throw new IllegalArgumentException("Amount>0");
        double old = balance;
        balance += amount;
        notifyObservers("deposit", String.format("Deposit %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override
    public void withdraw(double amount) {
        if(amount <= 0) throw new IllegalArgumentException("Amount>0");
        if(balance - amount < overdraftLimit) throw new IllegalStateException("Overdraft limit exceeded");
        double old = balance;
        balance -= amount;
        notifyObservers("withdraw", String.format("Withdraw %.2f (old: %.2f -> new: %.2f)", amount, old, balance));
    }

    @Override
    public void addObserver(NotificationObserver observer){ if(!observers.contains(observer)) observers.add(observer); }

    @Override
    public void removeObserver(NotificationObserver observer){ observers.remove(observer); }

    @Override
    public void notifyObservers(String event, String message){
        for(var o : new ArrayList<>(observers)){
            o.update(this, event, message);
        }
    }
}
