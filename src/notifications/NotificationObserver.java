package notifications;

import accounts.Account;

public interface NotificationObserver {
    void update(Account account, String event, String message);
}
