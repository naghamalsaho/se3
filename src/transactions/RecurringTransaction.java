package transactions;

import accounts.Account;
import java.time.LocalDateTime;

public class RecurringTransaction {
    private final Transaction.Type type;
    private final Account from;
    private final Account to;
    private final double amount;

    public RecurringTransaction(Transaction.Type type, Account from, Account to, double amount){
        this.type = type; this.from = from; this.to = to; this.amount = amount;
    }

    public Transaction toTransaction(){
        return new Transaction(type, from, to, amount);
    }
}
