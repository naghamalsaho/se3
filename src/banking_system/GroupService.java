package banking_system;

import accounts.Account;
import accounts.AccountGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupService {
    private final Map<String, Account> accounts;              // مرجع لخريطة الحسابات في التطبيق
    private final Map<String, AccountGroup> groups = new ConcurrentHashMap<>();
    private final Random idGen = new Random();

    public GroupService(Map<String, Account> accounts) {
        this.accounts = accounts;
    }

    public String createGroup(String label) {
        String gid;
        do {
            gid = "g" + (10000 + idGen.nextInt(90000));
        } while (groups.containsKey(gid) || accounts.containsKey(gid));
        AccountGroup ag = new AccountGroup(gid, label);
        groups.put(gid, ag);
        // ملاحظة: إضافة المجموعة كـ "حساب" في الخريطة (يسمح لك أن تُعاملها مثل حساب)
        accounts.put(gid, ag);
        return gid;
    }

    public boolean addToGroup(String gid, String accountId) {
        AccountGroup ag = groups.get(gid);
        Account a = accounts.get(accountId);
        if (ag == null || a == null) return false;
        ag.add(a);
        return true;
    }

    public boolean removeFromGroup(String gid, String accountId) {
        AccountGroup ag = groups.get(gid);
        Account a = accounts.get(accountId);
        if (ag == null || a == null) return false;
        ag.remove(a);
        return true;
    }

    public Optional<AccountGroup> getGroup(String gid) {
        return Optional.ofNullable(groups.get(gid));
    }

    public Collection<AccountGroup> listGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }
}
