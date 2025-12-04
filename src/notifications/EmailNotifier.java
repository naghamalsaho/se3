package notifications;

import accounts.Account;

public class EmailNotifier implements NotificationObserver {
    private final String email;
    public EmailNotifier(String email){ this.email = email; }

    @Override
    public void update(Account account, String event, String message) {
        System.out.printf("[EMAIL to %s] Account %s (%s): %s - %s%n", email, account.getName(), account.getId(), event, message);
    }
}
