package payment;

import transactions.Transaction;

public class PaymentService {
    private final PaymentGateway gateway;
    public PaymentService(PaymentGateway gateway){ this.gateway = gateway; }

    public boolean processExternalTransfer(Transaction tx){
        return gateway.process(tx);
    }
}
