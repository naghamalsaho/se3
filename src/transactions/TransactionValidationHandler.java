package transactions;

import accounts.Account;

public class TransactionValidationHandler extends TransactionHandler {
    @Override
    public boolean handle(Transaction tx) {
        // Basic validation: if TRANSFER or WITHDRAW, check from-account has enough balance
        if (tx.getType() == Transaction.Type.WITHDRAW || tx.getType() == Transaction.Type.TRANSFER) {
            Account from = tx.getFrom();
            if (from == null) {
                System.out.println("[Validation] No source account for withdraw/transfer");
                return false;
            }
            double available = from.getBalance();
            if (available < tx.getAmount()) {
                System.out.printf("[Validation] Insufficient funds: available=%.2f, required=%.2f%n", available, tx.getAmount());
                return false; // stop chain: reject transaction
            }
        }

        // You can add more validations: account state (frozen/closed), limits, AML checks, etc.

        // if valid, pass to next handler
        if (successor != null) {
            return successor.handle(tx);
        }
        // no successor but valid -> default approve
        return true;
    }
}
