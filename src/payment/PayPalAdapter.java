package payment;

import transactions.Transaction;

public class PayPalAdapter implements PaymentGateway {
    private final PayPalApi api;

    public PayPalAdapter(PayPalApi api){
        this.api = api;
    }

    @Override
    public boolean process(Transaction tx) {
        // adapt Transaction to PayPal API
        String from = tx.getFrom() != null ? tx.getFrom().getId() : "external";
        String to = tx.getTo() != null ? tx.getTo().getId() : "external";
        long cents = Math.round(tx.getAmount() * 100.0);
        return api.sendPayment(from, to, cents);
    }
}
