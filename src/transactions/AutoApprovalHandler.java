package transactions;

public class AutoApprovalHandler extends TransactionHandler {
    private final double limit;

    public AutoApprovalHandler(double limit){
        this.limit = limit;
    }

    @Override
    public boolean handle(Transaction tx) {
        if (tx.getAmount() <= limit) {
            System.out.println("[AutoApproval] Approved: " + tx.getAmount());
            return true;
        }

        if (successor != null) {
            System.out.println("[AutoApproval] Escalating: " + tx.getAmount());
            return successor.handle(tx);
        }

        return false;
    }
}
