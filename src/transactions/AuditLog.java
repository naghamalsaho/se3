package transactions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditLog {
    public static class Entry {
        public final Instant timestamp;
        public final Transaction tx;
        public final String status;
        public Entry(Transaction tx, String status){
            this.timestamp = Instant.now();
            this.tx = tx;
            this.status = status;
        }
    }
    private final List<Entry> entries = new ArrayList<>();
    public synchronized void record(Transaction tx, String status){
        entries.add(new Entry(tx, status));
        System.out.printf("[AUDIT] %s - %s - %s%n", Instant.now(), status, tx.getAmount());
    }
    public List<Entry> getEntries(){ return new ArrayList<>(entries); }
}
