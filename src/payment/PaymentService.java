// file: payment/PaymentService.java
package payment;

import transactions.Transaction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.Objects;

/**
 * PaymentService supports both sync and async external transfers.
 * - processExternalTransfer(tx) -> blocking
 * - processExternalTransferAsync(tx) -> CompletableFuture<Boolean> (non-blocking)
 */
public class PaymentService {
    private final PaymentGateway gateway;
    private final ExecutorService gatewayExecutor;

    public PaymentService(PaymentGateway gateway, ExecutorService gatewayExecutor){
        this.gateway = Objects.requireNonNull(gateway);
        this.gatewayExecutor = Objects.requireNonNull(gatewayExecutor);
    }

    // blocking call (keeps previous behavior)
    public boolean processExternalTransfer(Transaction tx){
        return gateway.process(tx);
    }

    // async: non-blocking; returns CompletableFuture<Boolean>
    public CompletableFuture<Boolean> processExternalTransferAsync(Transaction tx){
        return CompletableFuture.supplyAsync(() -> gateway.process(tx), gatewayExecutor);
    }

    // optional: shutdown executor if owned by this service
    public void shutdownExecutor(){
        try { gatewayExecutor.shutdown(); } catch (Exception ignored) {}
    }
}
