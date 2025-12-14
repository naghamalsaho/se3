package transactions;

public abstract class TransactionHandler {
    protected TransactionHandler successor;

    public void setSuccessor(TransactionHandler successor) {
        this.successor = successor;
    }

    public abstract boolean handle(Transaction tx);
}
