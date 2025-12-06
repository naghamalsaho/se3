package payment;

public class SWIFTApi {
    public boolean wireTransfer(String fromIban, String toIban, double amount, String currency) {
        System.out.printf("[SWIFTApi] wireTransfer from=%s to=%s %s %.2f%n", fromIban, toIban, currency, amount);
        return true;
    }
}