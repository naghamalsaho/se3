package recommendations;

import transactions.Transaction;
import java.util.*;
import accounts.Account;

public class RecommendationService {
    // very simple rules: if monthly outgoing > threshold -> suggest savings
    public List<String> analyze(List<Transaction> history, Account account){
        double out = 0;
        for(Transaction tx: history){
            if(tx.getFrom() != null && tx.getFrom().getId().equals(account.getId())) out += tx.getAmount();
        }
        List<String> recs = new ArrayList<>();
        if(out > 5000) recs.add("High spending detected. Consider opening a Savings plan or reducing recurring subscriptions.");
        if(account.getBalance() > 10000) recs.add("Consider Investment account for higher returns.");
        return recs;
    }
}
