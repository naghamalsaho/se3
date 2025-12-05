package admin;
import transactions.AuditLog;
import java.util.List;
import transactions.Transaction;
import transactions.AuditLog.Entry;

public class DashboardService {
    private final List<Transaction> transactions;
    private final AuditLog auditLog;
    public DashboardService(List<Transaction> txs, AuditLog auditLog){ this.transactions = txs; this.auditLog = auditLog; }
    public void printSummary(){
        System.out.println("=== DASHBOARD SUMMARY ===");
        System.out.println("Transactions total: " + transactions.size());
        System.out.println("Audit entries: " + auditLog.getEntries().size());
    }
}