package customers;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


public class CardService {
    private final Map<String, Card> cardsById = new ConcurrentHashMap<>();
    private final Map<String, String> cardIdByAccount = new ConcurrentHashMap<>();

    public CardService(){}

    public synchronized Card issueCard(String accountId, String holderName, LocalDate expiry) {
        // one card per account policy
        if (cardIdByAccount.containsKey(accountId)) {
            throw new IllegalStateException("Account already has a card: " + accountId);
        }
        // generate synthetic PAN (16 digits)
        String pan = generatePan();
        Card c = new Card(accountId, pan, holderName, expiry);
        cardsById.put(c.getId(), c);
        cardIdByAccount.put(accountId, c.getId());
        return c;
    }

    public boolean blockCard(String cardId) {
        Card c = cardsById.get(cardId);
        if (c == null) return false;
        if (c.getStatus() == Card.Status.CANCELLED) return false;
        c.setStatus(Card.Status.BLOCKED);
        return true;
    }

    public boolean unblockCard(String cardId) {
        Card c = cardsById.get(cardId);
        if (c == null) return false;
        if (c.getStatus() == Card.Status.CANCELLED) return false;
        c.setStatus(Card.Status.ACTIVE);
        return true;
    }

    public boolean cancelCard(String cardId) {
        Card c = cardsById.get(cardId);
        if (c == null) return false;
        c.setStatus(Card.Status.CANCELLED);
        // remove mapping by account
        cardIdByAccount.remove(c.getAccountId());
        return true;
    }

    public List<Card> listAll() {
        return new ArrayList<>(cardsById.values());
    }

    public List<Card> listByAccount(String accountId) {
        String cid = cardIdByAccount.get(accountId);
        if (cid == null) return Collections.emptyList();
        Card c = cardsById.get(cid);
        return c == null ? Collections.emptyList() : Collections.singletonList(c);
    }

    public boolean hasCardForAccount(String accountId) {
        return cardIdByAccount.containsKey(accountId);
    }

    public Optional<Card> getCardById(String id){ return Optional.ofNullable(cardsById.get(id)); }

    private String generatePan() {
        // simple random 16-digit, not real-world safe but fine for demo
        StringBuilder sb = new StringBuilder(16);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 16; i++) sb.append(rnd.nextInt(0,10));
        return sb.toString();
    }
}
