package accounts;

import notifications.NotificationObserver;
import java.util.ArrayList;
import java.util.List;

public class AccountGroup implements Account {
    private final String id;
    private final String name;
    private final List<Account> children = new ArrayList<>();
    public AccountGroup(String id, String name){
        this.id = id; this.name = name;
    }

    public void add(Account a){ children.add(a); }
    public void remove(Account a){ children.remove(a); }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }
    @Override public double getBalance(){
        return children.stream().mapToDouble(Account::getBalance).sum();
    }

    @Override
    public void deposit(double amount){ throw new UnsupportedOperationException("Deposit to group: deposit to child accounts individually"); }

    @Override public void withdraw(double amount){ throw new UnsupportedOperationException("Withdraw from group not supported"); }

    @Override
    public void addObserver(NotificationObserver observer){
        for(Account a : children) a.addObserver(observer);
    }

    @Override
    public void removeObserver(NotificationObserver observer){
        for(Account a : children) a.removeObserver(observer);
    }

    @Override
    public void notifyObservers(String event, String message){
        for(Account a : children) a.notifyObservers(event, message);
    }
}
