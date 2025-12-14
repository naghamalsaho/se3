package accounts;

import accounts.state.*;
import notifications.NotificationObserver;

import java.util.*;

/**
 * Composite account (group).
 */
public class AccountGroup implements Account {
    private final String id;
    private final String name;
    private final List<Account> children = new ArrayList<>();

    private DepositStrategy depositStrategy = new EvenSplitDeposit();
    private WithdrawStrategy withdrawStrategy = new SequentialWithdraw();

    public AccountGroup(String id, String name){
        this.id = id; this.name = name;
    }

    public void add(Account a){ children.add(a);  } // observer management remains outside ideally
    public void remove(Account a){ children.remove(a); }
    public List<Account> getChildren(){ return Collections.unmodifiableList(children); }

    @Override public String getId(){ return id; }
    @Override public String getName(){ return name; }
    @Override public double getBalance(){
        return children.stream().mapToDouble(Account::getBalance).sum();
    }

    @Override
    public void deposit(double amount){
        Map<Account, Double> plan = depositStrategy.splitDeposit(children, amount);
        for (Map.Entry<Account, Double> e : plan.entrySet()) {
            e.getKey().deposit(e.getValue());
        }
        notifyObservers("deposit", String.format("Group deposit %.2f distributed to %d children", amount, plan.size()));
    }

    @Override
    public void withdraw(double amount){
        Map<Account, Double> plan = withdrawStrategy.splitWithdraw(children, amount);
        for (Map.Entry<Account, Double> e : plan.entrySet()) {
            e.getKey().withdraw(e.getValue());
        }
        notifyObservers("withdraw", String.format("Group withdraw %.2f across %d children", amount, plan.size()));
    }

    @Override
    public void depositInternal(double amount) {
        // internal deposit delegates to same public behavior
        deposit(amount);
    }

    @Override
    public void withdrawInternal(double amount) {
        withdraw(amount);
    }

    @Override
    public void addObserver(NotificationObserver observer) {
        for (Account a : children) a.addObserver(observer);
    }

    @Override
    public void removeObserver(NotificationObserver observer) {
        for (Account a : children) a.removeObserver(observer);
    }

    @Override
    public void notifyObservers(String event, String message) {
        for (Account a : children) a.notifyObservers(event, message);
    }

    @Override
    public AccountStatus getStatus() {
        // aggregate policy: if any CLOSED -> CLOSED, else if any FROZEN -> FROZEN, else if any SUSPENDED -> SUSPENDED, else ACTIVE
        boolean anyClosed = false, anyFrozen = false, anySuspended = false;
        for (Account c : children) {
            String s = c.getStatusName();
            if ("CLOSED".equalsIgnoreCase(s)) anyClosed = true;
            else if ("FROZEN".equalsIgnoreCase(s)) anyFrozen = true;
            else if ("SUSPENDED".equalsIgnoreCase(s)) anySuspended = true;
        }
        if (anyClosed) return new ClosedState();
        if (anyFrozen) return new FrozenState();
        if (anySuspended) return new SuspendedState();
        return new ActiveState();
    }

    @Override
    public void setStatus(AccountStatus status) {
        for (Account c : children) {
            c.setStatus(status);
        }
    }

    @Override
    public String getStatusName() {
        return getStatus().name();
    }

    @Override
    public void freeze() {
        for (Account c : children) c.freeze();
    }

    @Override
    public void suspend() {
        for (Account c : children) c.suspend();
    }

    @Override
    public void close() {
        for (Account c : children) c.close();
    }

    @Override
    public void reopen() {
        for (Account c : children) c.reopen();
    }

    public DepositStrategy getDepositStrategy(){ return depositStrategy; }
    public WithdrawStrategy getWithdrawStrategy(){ return withdrawStrategy; }

    public void setDepositStrategy(DepositStrategy s){ this.depositStrategy = s; }
    public void setWithdrawStrategy(WithdrawStrategy s){ this.withdrawStrategy = s; }
}
