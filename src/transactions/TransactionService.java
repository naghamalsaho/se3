package transactions;

import accounts.Account;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class TransactionService {
    private final TransactionHandler approvalChain;
    private final List<Transaction> history = Collections.synchronizedList(new ArrayList<>());
    private final AuditLog auditLog = new AuditLog();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public TransactionService(TransactionHandler approvalChain){
        this.approvalChain = approvalChain;
    }

    public boolean process(Transaction tx){
        // validation + approval happens in chain (entry point should include validation)
        boolean approved = approvalChain.handle(tx);
        if(!approved){
            auditLog.record(tx, "REJECTED");
            return false;
        }
        try{
            // execute
            if(tx.getType() == Transaction.Type.DEPOSIT && tx.getTo() != null){
                tx.getTo().deposit(tx.getAmount());
            } else if(tx.getType() == Transaction.Type.WITHDRAW && tx.getFrom() != null){
                tx.getFrom().withdraw(tx.getAmount());
            } else if(tx.getType() == Transaction.Type.TRANSFER && tx.getFrom() != null && tx.getTo() != null){
                tx.getFrom().withdraw(tx.getAmount());
                tx.getTo().deposit(tx.getAmount());
            }
            history.add(tx);
            auditLog.record(tx, "EXECUTED");
            // large transaction notification hook (could be threshold-based)
            return true;
        }catch(Exception e){
            auditLog.record(tx, "FAILED: " + e.getMessage());
            return false;
        }
    }

    // schedule recurring tx: initialDelaySeconds, periodSeconds
    public ScheduledFuture<?> scheduleRecurring(RecurringTransaction rtx, long initialDelaySeconds, long periodSeconds){
        return scheduler.scheduleAtFixedRate(() -> process(rtx.toTransaction()), initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public List<Transaction> getHistory(){ return new ArrayList<>(history); }
    public AuditLog getAuditLog(){ return auditLog; }

    public void shutdown(){ scheduler.shutdown(); }
}
