package accounts;

import accounts.state.AccountStatus;
import accounts.state.ActiveState;
import notifications.NotificationObserver;

import java.util.ArrayList;
import java.util.List;

public class AccountGroup implements Account {
    private final String id;
    private final String name;
    private final List<Account> children = new ArrayList<>();

    // state for the group (propagate to children on set)
    private AccountStatus status = new ActiveState();

    public AccountGroup(String id, String name){
        this.id = id; this.name = name;
    }

    public void add(Account a){ children.add(a); }
    public void remove(Account a){ children.remove(a); }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }

    @Override
    public double getBalance(){
        return children.stream().mapToDouble(Account::getBalance).sum();
    }

    // Public operations on group are not supported (must operate on child accounts individually)
    @Override
    public void deposit(double amount) {
        throw new UnsupportedOperationException("Deposit to group: deposit to child accounts individually");
    }

    @Override
    public void withdraw(double amount) {
        throw new UnsupportedOperationException("Withdraw from group not supported");
    }

    // Internal ops required by Account interface - not applicable for group
    @Override
    public void depositInternal(double amount) {
        throw new UnsupportedOperationException("depositInternal not supported on AccountGroup");
    }

    @Override
    public void withdrawInternal(double amount) {
        throw new UnsupportedOperationException("withdrawInternal not supported on AccountGroup");
    }

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

    // -------------------- State management --------------------
    @Override
    public AccountStatus getStatus() {
        return status;
    }

    /**
     * Set status for the group and propagate to children.
     * Note: children might override status according to their own logic.
     */
    @Override
    public void setStatus(AccountStatus status) {
        this.status = status;
        // propagate to children
        for (Account a : children) {
            a.setStatus(status);
        }
    }

    @Override
    public String getStatusName() {
        return status.name();
    }

    @Override
    public void freeze() {
        setStatus(new accounts.state.FrozenState());
    }

    @Override
    public void suspend() {
        setStatus(new accounts.state.SuspendedState());
    }

    @Override
    public void close() {
        setStatus(new accounts.state.ClosedState());
    }

    @Override
    public void reopen() {
        setStatus(new accounts.state.ActiveState());
    }
}
