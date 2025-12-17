package customers;

import java.time.LocalDate;
import java.util.UUID;

public class Card {
    public enum Status { ACTIVE, BLOCKED, CANCELLED }

    private final String id;
    private final String accountId;
    private final String cardNumber; // PAN
    private final String holderName;
    private final LocalDate expiry;
    private Status status;

    public Card(String accountId, String cardNumber, String holderName, LocalDate expiry) {
        this.id = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.holderName = holderName;
        this.expiry = expiry;
        this.status = Status.ACTIVE;
    }

    public String getId(){ return id; }
    public String getAccountId(){ return accountId; }
    public String getCardNumber(){ return cardNumber; }
    public String getHolderName(){ return holderName; }
    public LocalDate getExpiry(){ return expiry; }
    public Status getStatus(){ return status; }
    public void setStatus(Status s){ this.status = s; }

    public String getMaskedPan() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    @Override
    public String toString(){
        return String.format("Card{id=%s, acct=%s, pan=%s, holder=%s, exp=%s, status=%s}",
                id, accountId, getMaskedPan(), holderName, expiry, status);
    }
}
