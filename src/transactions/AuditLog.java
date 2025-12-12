package transactions;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple AuditLog implementation:
 * - thread-safe collection of Entry
 * - record(tx, note) stores timestamp + tx summary + note
 * - entriesCount(), getEntries(), printRecent(int)
 * - Entry.toString() is human-friendly
 */
public class AuditLog {

    // thread-safe list for reads/writes by multiple threads
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public static class Entry {
        private final Instant timestamp;
        private final String action; // e.g., "EXECUTED", "REJECTED", "SKIPPED_INSUFFICIENT_FUNDS"
        private final String fromId;
        private final String toId;
        private final double amount;
        private final String note;

        public Entry(Instant timestamp, String action, String fromId, String toId, double amount, String note) {
            this.timestamp = timestamp;
            this.action = action;
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
            this.note = note;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getFrom() { return fromId; }
        public String getTo() { return toId; }
        public double getAmount() { return amount; }
        public String getNote() { return note; }

        @Override
        public String toString() {
            String ts = DateTimeFormatter.ISO_INSTANT.format(timestamp);
            String amt = String.format("%.2f", amount);
            String from = (fromId == null) ? "external" : fromId;
            String to = (toId == null) ? "external" : toId;
            String n = (note == null || note.isEmpty()) ? "" : " - " + note;
            return String.format("[%s] %s - %s -> %s : %s%s", ts, action, from, to, amt, n);
        }
    }

    public AuditLog(){}

    // record with Transaction object (you used auditLog.record(tx, "...") elsewhere)
    public void record(Transaction tx, String action) {
        if (tx == null) return;
        String from = tx.getFrom() != null ? tx.getFrom().getId() : null;
        String to = tx.getTo() != null ? tx.getTo().getId() : null;
        double amt = tx.getAmount();
        record(action, from, to, amt, null);
    }

    // overload with note
    public void record(Transaction tx, String action, String note) {
        if (tx == null) return;
        String from = tx.getFrom() != null ? tx.getFrom().getId() : null;
        String to = tx.getTo() != null ? tx.getTo().getId() : null;
        double amt = tx.getAmount();
        record(action, from, to, amt, note);
    }

    // low-level record
    public void record(String action, String fromId, String toId, double amount, String note) {
        Entry e = new Entry(Instant.now(), action, fromId, toId, amount, note);
        entries.add(e);
    }

    // convenience when code calls auditLog.record(tx, "SOME_NOTE")
    // ensure compatibility: keep previous single-arg record signature used in code
    public void record(Transaction tx, String action, Throwable t) {
        record(tx, action + ": " + (t == null ? "" : t.getMessage()));
    }

    // count and accessors
    public int entriesCount() {
        return entries.size();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    // print last N entries (most recent first)
    public void printRecent(int n) {
        if (n <= 0) n = 10;
        List<Entry> copy = new ArrayList<>(entries);
        int start = Math.max(0, copy.size() - n);
        for (int i = copy.size() - 1; i >= start; i--) {
            System.out.println(copy.get(i).toString());
        }
    }

    // backward compatible no-arg printRecent
    public void printRecent() { printRecent(10); }

    @Override
    public String toString() {
        return "AuditLog{entries=" + entries.size() + "}";
    }
}
