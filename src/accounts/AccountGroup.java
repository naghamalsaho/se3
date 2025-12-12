package accounts;

import accounts.state.AccountStatus;
import notifications.NotificationObserver;

import java.util.*;

/**
 * Composite account (family / group).
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

    public void add(Account a){ children.add(a); }
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

    // group does not use internal deposit/withdraw directly (no-op)
    @Override public void depositInternal(double amount) { /* no-op */ }
    @Override public void withdrawInternal(double amount) { /* no-op */ }

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

    // Aggregate status: CLOSED > SUSPENDED > FROZEN > ACTIVE
    @Override
    public AccountStatus getStatus() {
        return null; // keep null or implement a GroupStatus class if you want
    }

    @Override
    public void setStatus(AccountStatus status) {
        // propagate to children: optional — keep empty or implement as needed
    }

    @Override
    public String getStatusName() {
        boolean anyClosed = false, anySuspended = false, anyFrozen = false;
        if (children.isEmpty()) return "ACTIVE";
        for (Account a : children) {
            String s = a.getStatusName();
            if ("CLOSED".equalsIgnoreCase(s)) { anyClosed = true; break; }
            if ("SUSPENDED".equalsIgnoreCase(s)) anySuspended = true;
            if ("FROZEN".equalsIgnoreCase(s)) anyFrozen = true;
        }
        if (anyClosed) return "CLOSED";
        if (anySuspended) return "SUSPENDED";
        if (anyFrozen) return "FROZEN";
        return "ACTIVE";
    }

    @Override
    public void freeze() {
        for (Account a : children) a.freeze();
    }

    @Override
    public void suspend() {
        for (Account a : children) a.suspend();
    }

    @Override
    public void close() {
        for (Account a : children) a.close();
    }

    @Override
    public void reopen() {
        for (Account a : children) a.reopen();
    }

    public DepositStrategy getDepositStrategy(){ return depositStrategy; }
    public WithdrawStrategy getWithdrawStrategy(){ return withdrawStrategy; }

    public void setDepositStrategy(DepositStrategy s){ this.depositStrategy = s; }
    public void setWithdrawStrategy(WithdrawStrategy s){ this.withdrawStrategy = s; }
}
