package transactions;

public class ManagerApprovalHandler extends TransactionHandler {
    private final double limit;

    public ManagerApprovalHandler(double limit){ this.limit = limit; }

    @Override
    public boolean handle(Transaction tx) {
        if(tx.getAmount() <= limit){
            System.out.println("[ManagerApproval] Manager approved: " + tx.getAmount());
            return true;
        } else if(successor != null){
            System.out.println("[ManagerApproval] Escalating to next: " + tx.getAmount());
            return successor.handle(tx);
        }
        return false;
    }
}
