package banking_system;

import accounts.*;
import interest.*;
import notifications.*;
import transactions.*;

public class BankFacade {
    // simplified facade for typical operations
    private final TransactionHandler approvalChain;

    public BankFacade(TransactionHandler approvalChain) {
        this.approvalChain = approvalChain;
    }

    public boolean transfer(Account from, Account to, double amount){
        Transaction tx = new Transaction(Transaction.Type.TRANSFER, from, to, amount);
        boolean approved = approvalChain.handle(tx);
        if(!approved) {
            System.out.println("Transfer was not approved");
            return false;
        }
        try {
            from.withdraw(amount);
            to.deposit(amount);
            System.out.println("Transfer executed");
            return true;
        } catch(Exception e){
            System.out.println("Error performing transfer: " + e.getMessage());
            return false;
        }
    }

    public void deposit(Account to, double amount){
        Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, to, amount);
        if(approvalChain.handle(tx)) to.deposit(amount);
    }
}
