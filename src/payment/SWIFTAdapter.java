package payment;

import transactions.Transaction;

public class SWIFTAdapter implements PaymentGateway {
    private final SWIFTApi api;
    public SWIFTAdapter(SWIFTApi api){ this.api = api; }

    @Override
    public boolean process(Transaction tx) {
        // For demo use ids as IBANs and "USD"
        String from = tx.getFrom() != null ? tx.getFrom().getId() : "EXTERNAL";
        String to = tx.getTo() != null ? tx.getTo().getId() : "EXTERNAL";
        return api.wireTransfer(from, to, tx.getAmount(), "USD");
    }
}
