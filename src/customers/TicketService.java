package customers;

import java.util.*;

public class TicketService {
    private final Map<String, Ticket> tickets = new HashMap<>();
    public Ticket create(String userId, String subject, String desc){
        Ticket t = new Ticket(userId, subject, desc);
        tickets.put(t.id, t);
        System.out.println("[TICKET] Created " + t.id);
        return t;
    }
    public void addMessage(String ticketId, String message){
        Ticket t = tickets.get(ticketId);
        if(t != null) t.messages.add(message);
    }
    public void close(String ticketId){ Ticket t = tickets.get(ticketId); if(t!=null) t.status = Ticket.Status.CLOSED; }
    public List<Ticket> listOpen(){ var out = new ArrayList<Ticket>(); for(Ticket t: tickets.values()) if(t.status!=Ticket.Status.CLOSED) out.add(t); return out; }
}