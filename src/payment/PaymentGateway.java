package payment;

import transactions.Transaction;

public interface PaymentGateway {
    /**
     * Process an external payment (e.g., cross-border or card).
     * Return true if processed successfully.
     */
    boolean process(Transaction tx);
}
