package admin;

import transactions.AuditLog;

public class ReportingService {
    private final AuditLog audit;

    public ReportingService(AuditLog audit){
        this.audit = audit;
    }

    public void dailyReport(){
        System.out.println("=== Daily Audit Log ===");

        audit.getEntries().forEach(e -> {
            String ts = e.getTimestamp().toString();
            String action = e.getAction();
            double amount = e.getAmount(); // <-- استخدم getter الصحيح
            String from = e.getFrom();
            String to = e.getTo();
            String note = e.getNote();

            System.out.println(ts + " " + action + " " + from + " -> " + to + " : " + String.format("%.2f", amount) + (note == null ? "" : " - " + note));
        });
    }
}
