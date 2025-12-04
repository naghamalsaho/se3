package notifications;

import accounts.Account;

public class SMSNotifier implements NotificationObserver {
    private final String phone;
    public SMSNotifier(String phone){ this.phone = phone; }

    @Override
    public void update(Account account, String event, String message) {
        System.out.printf("[SMS to %s] Account %s: %s - %s%n", phone, account.getName(), event, message);
    }
}
