package customers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {
    public enum Status { OPEN, IN_PROGRESS, CLOSED }
    public final String id;
    public final String userId;
    public final String subject;
    public final String description;
    public Status status;
    public final Instant createdAt;
    public final List<String> messages = new ArrayList<>();

    public Ticket(String userId, String subject, String desc){
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.subject = subject;
        this.description = desc;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }
}
