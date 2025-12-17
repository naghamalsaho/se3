// src/customers/Ticket.java
package customers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {
    public enum Status { PENDING, APPROVED, REJECTED, CLOSED }

    public final String id;
    public final String userId;      // من طلب التيكيت
    public final String accountId;   // على أي حساب هذا الطلب
    public final String subject;
    public final String description;
    public Status status;
    public final List<String> messages = new ArrayList<>();
    public String issuedCardId;      // يملأ بعد الموافقة
    public final Instant createdAt = Instant.now();

    public Ticket(String userId, String accountId, String subject, String description) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.accountId = accountId;
        this.subject = subject;
        this.description = description;
        this.status = Status.PENDING;
        this.issuedCardId = null;
        this.messages.add("Created at " + createdAt);
    }
}
