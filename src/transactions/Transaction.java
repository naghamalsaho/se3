package transactions;

import accounts.Account;

public class Transaction {
    public enum Type { DEPOSIT, WITHDRAW, TRANSFER }
    private final Type type;
    private final Account from;
    private final Account to; // may be null
    private final double amount;

    public Transaction(Type type, Account from, Account to, double amount){
        this.type = type; this.from = from; this.to = to; this.amount = amount;
    }

    public Type getType(){ return type; }
    public Account getFrom(){ return from; }
    public Account getTo(){ return to; }
    public double getAmount(){ return amount; }
}
