package transactions;

public abstract class TransactionHandler {
    protected TransactionHandler successor;
    public void setSuccessor(TransactionHandler s){ this.successor = s; }
    public abstract boolean handle(Transaction tx);
}
