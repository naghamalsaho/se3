package admin;
import transactions.AuditLog;
import java.util.List;

public class ReportingService {
    private final AuditLog audit;
    public ReportingService(AuditLog audit){ this.audit = audit; }
    public void dailyReport(){
        System.out.println("=== Daily Audit Log ===");
        audit.getEntries().forEach(e -> System.out.println(e.timestamp + " " + e.status + " " + e.tx.getAmount()));
    }
}