// src/transactions/TransactionValidationHandler.java
package transactions;

import accounts.Account;

/**
 * Validation handler in the approval chain.
 * Returns boolean (true = ok / approved to next step).
 */
public class TransactionValidationHandler extends TransactionHandler {

    @Override
    public boolean handle(Transaction tx) {
        // 1) Basic amount sanity
        if (tx == null || tx.getAmount() <= 0) {
            System.out.println("[Validation] Invalid amount: " + (tx == null ? "null" : tx.getAmount()));
            return false;
        }

        Account from = tx.getFrom();
        Account to = tx.getTo();

        // 2) check source status
        if (from != null) {
            if (!from.getStatus().canBeSource()) {
                System.out.printf("[Validation] Source account %s status=%s => rejected%n",
                        from.getId(), from.getStatusName());
                return false;
            }
        }

        // 3) check destination status
        if (to != null) {
            if (!to.getStatus().canReceive()) {
                System.out.printf("[Validation] Destination account %s status=%s => rejected%n",
                        to.getId(), to.getStatusName());
                return false;
            }
        }

        // 4) Balance (available) check for withdraw/transfer
        if (tx.getType() == Transaction.Type.WITHDRAW || tx.getType() == Transaction.Type.TRANSFER) {
            if (from == null) {
                System.out.println("[Validation] No source account for withdraw/transfer");
                return false;
            }
            try {
                // <-- use available balance (allows decorators to increase available amount)
                double available = from.getAvailableBalance();
                if (available < tx.getAmount()) {
                    System.out.printf("[Validation] Insufficient funds: available=%.2f, required=%.2f%n",
                            available, tx.getAmount());
                    return false;
                }
            } catch (Exception e) {
                System.out.println("[Validation] Failed to read available balance: " + e.getMessage());
                return false;
            }
        }

        // pass to successor in the chain (boolean chain)
        if (successor != null) {
            return successor.handle(tx);
        }

        return true; // no successor => approved
    }
}
