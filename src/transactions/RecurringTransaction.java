package transactions;

import accounts.Account;

import java.util.UUID;

public class RecurringTransaction {
    private final String id = UUID.randomUUID().toString();
    private final Transaction.Type type;
    private final Account from;
    private final Account to;
    private final double amount;

    public RecurringTransaction(Transaction.Type type, Account from, Account to, double amount){
        this.type = type;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    /** --- getters added so the scheduler/task can inspect the recurring transaction --- */
    public String getId() {
        return id;
    }

    public Transaction.Type getType() {
        return type;
    }

    public Account getFrom() {
        return from;
    }

    public Account getTo() {
        return to;
    }

    public double getAmount() {
        return amount;
    }

    public Transaction toTransaction(){
        return new Transaction(type, from, to, amount);
    }

    @Override
    public String toString() {
        String fromId = from != null ? from.getId() : "external";
        String toId = to != null ? to.getId() : "external";
        return String.format("RTX[%s] %s %s->%s %.2f", id, type, fromId, toId, amount);
    }
}
