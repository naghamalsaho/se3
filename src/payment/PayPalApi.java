package payment;

public class PayPalApi {

    // هذا مجرد تمثيل لوظيفة خارجية (Legacy/External System)
    public boolean sendPayment(String fromAccount, String toAccount, long amountCents) {
        System.out.println("[PayPalAPI] Sending " + amountCents + " cents from "
                + fromAccount + " to " + toAccount);
        return true; // دائماً ناجح لأغراض تجريب الـAdapter
    }
}
