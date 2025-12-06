package accounts.decorators;

import accounts.Account;
import accounts.state.AccountStatus;
import notifications.NotificationObserver;

import java.util.List;

public abstract class AccountDecorator implements Account {
    protected final Account wrapped;

    public AccountDecorator(Account wrapped){
        this.wrapped = wrapped;
    }

    // Delegate all methods to wrapped by default
    @Override public String getId(){ return wrapped.getId(); }
    @Override public String getName(){ return wrapped.getName(); }
    @Override public double getBalance(){ return wrapped.getBalance(); }

    @Override public void deposit(double amount){ wrapped.deposit(amount); }
    @Override public void withdraw(double amount){ wrapped.withdraw(amount); }

    @Override public void depositInternal(double amount){ wrapped.depositInternal(amount); }
    @Override public void withdrawInternal(double amount){ wrapped.withdrawInternal(amount); }

    @Override public void addObserver(NotificationObserver observer){ wrapped.addObserver(observer); }
    @Override public void removeObserver(NotificationObserver observer){ wrapped.removeObserver(observer); }
    @Override public void notifyObservers(String event, String message){ wrapped.notifyObservers(event, message); }

    @Override public AccountStatus getStatus(){ return wrapped.getStatus(); }
    @Override public void setStatus(AccountStatus status){ wrapped.setStatus(status); }
    @Override public String getStatusName(){ return wrapped.getStatusName(); }

    @Override public void freeze(){ wrapped.freeze(); }
    @Override public void suspend(){ wrapped.suspend(); }
    @Override public void close(){ wrapped.close(); }
    @Override public void reopen(){ wrapped.reopen(); }
}
