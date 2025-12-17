package customers;

import accounts.Account;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicketService {
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Map<String, Account> accounts;     // مرجع لخريطة الحسابات
    private final CardService cardService;

    public TicketService(Map<String, Account> accounts, CardService cardService){
        this.accounts = accounts;
        this.cardService = cardService;
    }

    // create ticket for card request (accountId required)
    public Ticket create(String userId, String accountId, String subject, String desc){
        if (!accounts.containsKey(accountId)) {
            throw new IllegalArgumentException("Unknown account: " + accountId);
        }
        // optional policy: reject immediate if account already has card
        if (cardService.hasCardForAccount(accountId)) {
            Ticket t = new Ticket(userId, accountId, subject, desc);
            t.status = Ticket.Status.REJECTED;
            t.messages.add("Rejected: account already has card");
            tickets.put(t.id, t);
            return t;
        }
        Ticket t = new Ticket(userId, accountId, subject, desc);
        tickets.put(t.id, t);
        return t;
    }

    public List<Ticket> listOpen(){
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (t.status == Ticket.Status.PENDING) out.add(t);
        }
        out.sort(Comparator.comparing(a -> a.createdAt));
        return out;
    }

    public Ticket get(String id) { return tickets.get(id); }

    // approve ticket => issue card and update ticket
    public Card approve(String ticketId, String adminUserId) {
        Ticket t = tickets.get(ticketId);
        if (t == null) throw new IllegalArgumentException("Unknown ticket: " + ticketId);
        if (t.status != Ticket.Status.PENDING) throw new IllegalStateException("Ticket not pending: " + t.status);

        Account acct = accounts.get(t.accountId);
        String holder = acct != null && acct.getName() != null ? acct.getName() : "Card Holder";
        LocalDate expiry = LocalDate.now().plusYears(3);
        Card card = cardService.issueCard(t.accountId, holder, expiry);

        t.status = Ticket.Status.APPROVED;
        t.messages.add("Approved by " + adminUserId + " -> card=" + card.getId());
        return card;
    }

    public void reject(String ticketId, String adminUserId, String reason) {
        Ticket t = tickets.get(ticketId);
        if (t == null) throw new IllegalArgumentException("Unknown ticket: " + ticketId);
        if (t.status != Ticket.Status.PENDING) throw new IllegalStateException("Ticket not pending: " + t.status);
        t.status = Ticket.Status.REJECTED;
        t.messages.add("Rejected by " + adminUserId + " reason=" + (reason==null? "n/a": reason));
    }
}
