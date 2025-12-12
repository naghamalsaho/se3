package transactions;

import accounts.Account;

public class TransactionValidationHandler extends TransactionHandler {

    @Override
    public boolean handle(Transaction tx) {
        // 1) Basic sanity
        if (tx.getAmount() <= 0) {
            System.out.println("[Validation] Invalid amount: " + tx.getAmount());
            return false;
        }

        Account from = tx.getFrom();
        Account to = tx.getTo();

        // 2) SOURCE account rules (if there's a source)
        if (from != null) {
            String sFrom = from.getStatusName(); // ACTIVE, FROZEN, SUSPENDED, CLOSED

            // CLOSED => cannot be source at all
            if ("CLOSED".equalsIgnoreCase(sFrom)) {
                System.out.printf("[Validation] Source account %s is CLOSED => cannot be source of %s%n", from.getId(), tx.getType());
                return false;
            }

            // FROZEN => cannot be source of any outgoing financial op
            if ("FROZEN".equalsIgnoreCase(sFrom)) {
                if (tx.getType() == Transaction.Type.WITHDRAW || tx.getType() == Transaction.Type.TRANSFER) {
                    System.out.printf("[Validation] Source account %s is FROZEN => cannot be source of %s%n", from.getId(), tx.getType());
                    return false;
                }
            }

            // SUSPENDED => disallow outgoing (withdraw/transfer) but allow being source for other non-fin ops (typically none)
            if ("SUSPENDED".equalsIgnoreCase(sFrom)) {
                if (tx.getType() == Transaction.Type.WITHDRAW || tx.getType() == Transaction.Type.TRANSFER) {
                    System.out.printf("[Validation] Source account %s is SUSPENDED => outgoing operations not allowed%n", from.getId());
                    return false;
                }
            }
        }

        // 3) DESTINATION account rules (if there's a destination)
        if (to != null) {
            String sTo = to.getStatusName();

            // CLOSED => cannot receive deposits/transfers
            if ("CLOSED".equalsIgnoreCase(sTo)) {
                System.out.printf("[Validation] Destination account %s is CLOSED => cannot receive %s%n", to.getId(), tx.getType());
                return false;
            }

            // FROZEN => strict: do not accept deposits/transfers
            if ("FROZEN".equalsIgnoreCase(sTo)) {
                if (tx.getType() == Transaction.Type.DEPOSIT || tx.getType() == Transaction.Type.TRANSFER) {
                    System.out.printf("[Validation] Destination account %s is FROZEN => deposits/transfers not allowed%n", to.getId());
                    return false;
                }
            }

            // SUSPENDED => allow incoming deposits/transfers (so owner can top-up)
            // no action required (explicit for clarity)
            if ("SUSPENDED".equalsIgnoreCase(sTo)) {
                // allowed
            }
        }

        // 4) Balance check for withdraw/transfer (only if source allowed)
        if (tx.getType() == Transaction.Type.WITHDRAW || tx.getType() == Transaction.Type.TRANSFER) {
            if (from == null) {
                System.out.println("[Validation] No source account for withdraw/transfer");
                return false;
            }
            try {
                double available = from.getBalance();
                if (available < tx.getAmount()) {
                    System.out.printf("[Validation] Insufficient funds: available=%.2f, required=%.2f%n", available, tx.getAmount());
                    return false;
                }
            } catch (Exception e) {
                System.out.println("[Validation] Failed to read balance: " + e.getMessage());
                return false;
            }
        }

        // 5) pass to successor if exists
        if (successor != null) return successor.handle(tx);
        return true;
    }
}
