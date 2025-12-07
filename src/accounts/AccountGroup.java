package accounts;

import accounts.state.AccountStatus;
import notifications.NotificationObserver;

import java.util.*;

/**
 * Composite account (family / group).
 * Now supports deposit/withdraw by delegating to children using strategies.
 */
public class AccountGroup implements Account {
    private final String id;
    private final String name;
    private final List<Account> children = new ArrayList<>();

    // default strategies (can be changed by caller)
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
        // compute distribution then call deposit on children
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

    }

    @Override
    public void withdrawInternal(double amount) {

    }

    // observer management: attach observer to all children
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
        return null;
    }

    @Override
    public void setStatus(AccountStatus status) {

    }

    @Override
    public String getStatusName() {
        return "";
    }

    @Override
    public void freeze() {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void close() {

    }

    @Override
    public void reopen() {

    }

    // strategy setters
    public void setDepositStrategy(DepositStrategy s){ this.depositStrategy = s; }
    public void setWithdrawStrategy(WithdrawStrategy s){ this.withdrawStrategy = s; }
}
