//package accounts;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import accounts.factory.AccountFactory;
//
///**
// * AccountService: مسؤول عن إنشاء الحسابات والتحقّق من التكرار (id & owner name).
// * يقوم بحجز اسم المالك بشكل ذري لمنع duplicates عند التوازي.
// */
//public class AccountService {
//
//    private final Map<String, Account> accounts;
//    // set of normalized owner names to prevent duplicates atomically
//    private final Set<String> ownerNames = ConcurrentHashMap.newKeySet();
//
//    public AccountService(Map<String, Account> accounts) {
//        // ensure thread-safe map
//        if (!(accounts instanceof ConcurrentHashMap)) {
//            this.accounts = new ConcurrentHashMap<>(accounts);
//        } else {
//            this.accounts = accounts;
//        }
//    }
//
//    public Map<String, Account> getAccountsMap() { return accounts; }
//
//    private boolean idTaken(String id) {
//        if (id == null || id.isBlank()) return false;
//        return accounts.containsKey(id);
//    }
//
//    private String normalizeOwner(String name) {
//        return name == null ? null : name.trim().toLowerCase();
//    }
//
//    // Create savings account — atomic reservation of owner name
//    public Account createSavings(String requestedId, String owner, double initial) {
//        String ownerKey = normalizeOwner(owner);
//        // try reserve owner name atomically
//        if (ownerKey != null && !ownerNames.add(ownerKey)) {
//            throw new IllegalArgumentException("Account owner name already exists: " + owner);
//        }
//
//        try {
//            String useId = (requestedId == null || requestedId.isBlank()) ? null : requestedId.trim();
//            if (useId != null && idTaken(useId)) {
//                throw new IllegalArgumentException("Account id already exists: " + useId);
//            }
//
//            // delegate to factory
//            Account a = AccountFactory.createSavings(useId, owner, initial);
//
//            // put if absent to avoid race on id
//            Account conflict = accounts.putIfAbsent(a.getId(), a);
//            if (conflict != null) {
//                throw new IllegalStateException("Generated account id already exists: " + a.getId());
//            }
//            return a;
//        } catch (RuntimeException ex) {
//            // rollback owner name reservation on failure
//            if (ownerKey != null) ownerNames.remove(ownerKey);
//            throw ex;
//        }
//    }
//
//    public Account createChecking(String requestedId, String owner, double initial) {
//        String ownerKey = normalizeOwner(owner);
//        if (ownerKey != null && !ownerNames.add(ownerKey)) {
//            throw new IllegalArgumentException("Account owner name already exists: " + owner);
//        }
//        try {
//            String useId = (requestedId == null || requestedId.isBlank()) ? null : requestedId.trim();
//            if (useId != null && idTaken(useId)) {
//                throw new IllegalArgumentException("Account id already exists: " + useId);
//            }
//            Account a = AccountFactory.createChecking(useId, owner, initial);
//            Account conflict = accounts.putIfAbsent(a.getId(), a);
//            if (conflict != null) {
//                throw new IllegalStateException("Generated account id already exists: " + a.getId());
//            }
//            return a;
//        } catch (RuntimeException ex) {
//            if (ownerKey != null) ownerNames.remove(ownerKey);
//            throw ex;
//        }
//    }
//
//    public Account createLoan(String requestedId, String owner, double initial, double rate) {
//        String ownerKey = normalizeOwner(owner);
//        if (ownerKey != null && !ownerNames.add(ownerKey)) {
//            throw new IllegalArgumentException("Account owner name already exists: " + owner);
//        }
//        try {
//            String useId = (requestedId == null || requestedId.isBlank()) ? null : requestedId.trim();
//            if (useId != null && idTaken(useId)) {
//                throw new IllegalArgumentException("Account id already exists: " + useId);
//            }
//            Account a = AccountFactory.createLoan(useId, owner, initial, rate);
//            Account conflict = accounts.putIfAbsent(a.getId(), a);
//            if (conflict != null) {
//                throw new IllegalStateException("Generated account id already exists: " + a.getId());
//            }
//            return a;
//        } catch (RuntimeException ex) {
//            if (ownerKey != null) ownerNames.remove(ownerKey);
//            throw ex;
//        }
//    }
//
//    public Account createInvestment(String requestedId, String owner, double initial, String portfolio) {
//        String ownerKey = normalizeOwner(owner);
//        if (ownerKey != null && !ownerNames.add(ownerKey)) {
//            throw new IllegalArgumentException("Account owner name already exists: " + owner);
//        }
//        try {
//            String useId = (requestedId == null || requestedId.isBlank()) ? null : requestedId.trim();
//            if (useId != null && idTaken(useId)) {
//                throw new IllegalArgumentException("Account id already exists: " + useId);
//            }
//            Account a = AccountFactory.createInvestment(useId, owner, initial, portfolio);
//            Account conflict = accounts.putIfAbsent(a.getId(), a);
//            if (conflict != null) {
//                throw new IllegalStateException("Generated account id already exists: " + a.getId());
//            }
//            return a;
//        } catch (RuntimeException ex) {
//            if (ownerKey != null) ownerNames.remove(ownerKey);
//            throw ex;
//        }
//    }
//
//    public boolean existsById(String id){ return id != null && accounts.containsKey(id); }
//    public boolean existsByName(String name){ return name != null && ownerNames.contains(normalizeOwner(name)); }
//}
