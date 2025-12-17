package banking_system;
import customers.Card;
import customers.CardService;
import accounts.*;
import accounts.decorators.InsuranceDecorator;
import accounts.decorators.OverdraftProtectionDecorator;
import accounts.factory.AccountFactory;
import customers.Ticket;
import customers.TicketService;

import notifications.EmailNotifier;
import notifications.SMSNotifier;
import payment.*;
import recommendations.RecommendationService;
import security.AuthService;
import security.Role;
import transactions.AuditLog;
import transactions.RecurringTransaction;
import transactions.Transaction;
import transactions.TransactionService;
import util.LocalizationService;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interactive CLI for the banking system.
 * ملاحظة: تمّت إزالة طلب السرّ للأدمن — الآن هناك حساب ثابت "admin".
 * هذا الملف مُحدَّث: تصحيح دوال الغروبات، صلاحيات، واستخدام facade بشكل صحيح.
 */
public class InteractiveConsole {
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Account> accounts; // id -> Account
    private final TransactionService txService;
    private final BankingFacade facade;
    private final AuthService auth;
    private final TicketService ticketService;
    private static final String ADMIN_SECRET = "admin"; // غيّري إذا بدك
    private static final String ADMIN_USER = "admin";
    // notifiers

    private final EmailNotifier emailNotifier = new EmailNotifier("ops@bank.com");
    private final SMSNotifier smsNotifier = new SMSNotifier("+12345");
    // new: groups storage (id -> set of account ids)
    private final Map<String, accounts.AccountGroup> groups = new ConcurrentHashMap<>();
    private final Random idGen = new Random();
    private final PaymentService paymentService;
    private final GroupService groupService;
    private final LocalizationService loc;
    private final CardService cardService;

    public InteractiveConsole(Map<String, Account> accounts,
                              TransactionService txService,
                              BankingFacade facade,
                              AuthService auth,
                              TicketService ticketService,
                              PaymentService paymentService,
                              LocalizationService loc,
                              CardService cardService ) {
        this.accounts = accounts;
        this.txService = txService;
        this.facade = facade;
        this.auth = auth;
        this.ticketService = ticketService;
        this.paymentService = paymentService;
        this.groupService = new GroupService(accounts);
        this.loc = loc;
        this.cardService=cardService;
    }

    public void start() {
        // ensure fixed admin user exists in AuthService? we'll register on demand
        System.out.println(loc.t("menu.title"));
        // نفتح القائمة فوراً كمستخدم افتراضي ("customer") — هذا المُستخدم يمكن تغييره لاحقاً
        String currentUserId = "customer";
        // Ensure default user registered as CUSTOMER (idempotent)
        if (!auth.authorize(currentUserId, Role.CUSTOMER)) {
            auth.register(currentUserId, Role.CUSTOMER);
        }

        // attach notifiers by default to existing accounts
        accounts.values().forEach(a -> { a.addObserver(emailNotifier); a.addObserver(smsNotifier); });

        boolean running = true;
        while (running) {
            printMainMenu(currentUserId);
            String choice = scanner.nextLine().trim();
            try {
                switch (choice.toLowerCase()) {
                    case "1": cmdCreateAccount(currentUserId); break;
                    case "2": cmdListAccounts(); break;
                    case "3": cmdDeposit(currentUserId); break;
                    case "4": cmdWithdraw(currentUserId); break;
                    case "5": cmdTransfer(currentUserId); break;
                    case "6": cmdScheduleRecurring(currentUserId); break;
                    case "7": cmdViewHistory(); break;
                    case "8": cmdCreateTicket(currentUserId); break;
                    case "9": cmdDecorateAccount(); break;
                    case "10": cmdCreateAccountGroup(currentUserId); break;
                    case "11": cmdAddAccountToGroup(currentUserId); break;
                    case "12": cmdRemoveAccountFromGroup(currentUserId); break;
                    case "13": cmdDepositToGroup(currentUserId); break;
                    case "14": cmdWithdrawFromGroup(currentUserId); break;
                    case "15": cmdApplyInterestToGroup(currentUserId); break;
                    case "16": cmdExternalTransfer(currentUserId); break;
                    case "d":
                    case "admin":
                        System.out.print("Enter admin secret: ");
                        String secret = scanner.nextLine().trim();
                        if (ADMIN_SECRET.equals(secret)) {
                            // Ensure admin user is registered with ADMIN role (idempotent)
                            if (!auth.authorize(ADMIN_USER, Role.ADMIN)) {
                                auth.register(ADMIN_USER, Role.ADMIN);
                                System.out.println("Admin account registered as: " + ADMIN_USER);
                            }
                            // now open dashboard as the admin user
                            cmdAdminDashboard(ADMIN_USER);
                        } else {
                            System.out.println("Unauthorized: invalid admin secret.");
                        }
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }

        System.out.println("Exiting CLI. Bye!");
    }
    private void printMainMenu(String userId) {
        System.out.println(loc.t("menu.title2"));
        System.out.println(loc.t("menu.create_account"));
        System.out.println(loc.t("menu.list_accounts"));
        System.out.println(loc.t("menu.deposit"));
        System.out.println(loc.t("menu.withdraw"));
        System.out.println(loc.t("menu.transfer"));
        System.out.println(loc.t("menu.schedule_recurring"));
        System.out.println(loc.t("menu.history"));
        System.out.println(loc.t("menu.create_ticket"));
        System.out.println(loc.t("menu.add_feature"));
        System.out.println(loc.t("menu.create_group"));
        System.out.println(loc.t("menu.add_to_group"));
        System.out.println(loc.t("menu.remove_from_group"));
        System.out.println(loc.t("menu.deposit_to_group"));
        System.out.println(loc.t("menu.withdraw_from_group"));
        System.out.println(loc.t("menu.apply_interest_group"));
        System.out.println(loc.t("menu.external_transfer"));
        // hint for admin entry
        System.out.println(loc.t("menu.admin_dashboard_hint"));
        System.out.println(loc.t("menu.exit"));
        System.out.print(loc.t("menu.prompt"));
    }


    /* -------------------------
       Account creation & basic operations
       (auto-id via factory)
       ------------------------- */
    private void cmdCreateAccount(String currentUserId) {
        System.out.println("Choose type: 1) Savings 2) Checking 3) Loan 4) Investment");
        String t = scanner.nextLine().trim();
        String id = null;
        System.out.print("Enter account owner/name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Owner name is required.");
            return;
        }
        String nameKey = name.toLowerCase().trim();

        System.out.print("Initial amount (number): ");
        double initial;
        try {
            initial = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid initial amount.");
            return;
        }

        // check owner name uniqueness
        boolean nameExists = accounts.values().stream()
                .anyMatch(a -> a.getName() != null && a.getName().trim().toLowerCase().equals(nameKey));
        if (nameExists) {
            System.out.println("Cannot create account: owner name already exists: " + name);
            return;
        }

        Account a;
        try {
            switch (t) {
                case "1":
                    a = AccountFactory.createSavings(id, name, initial);
                    break;
                case "2":
                    a = AccountFactory.createChecking(id, name, initial);
                    break;
                case "3":
                    System.out.print("Enter loan interest rate (yearly %): ");
                    double rate = Double.parseDouble(scanner.nextLine().trim());
                    a = AccountFactory.createLoan(id, name, initial, rate);
                    break;
                case "4":
                    System.out.print("Enter portfolio type (e.g. balanced): ");
                    String p = scanner.nextLine().trim();
                    if (p.isEmpty()) p = "balanced";
                    a = AccountFactory.createInvestment(id, name, initial, p);
                    break;
                default:
                    System.out.println("Unknown type");
                    return;
            }
        } catch (Exception e) {
            System.out.println("Error creating account object: " + e.getMessage());
            return;
        }

        Account existing = accounts.putIfAbsent(a.getId(), a);
        if (existing != null) {
            System.out.println("Cannot register account: generated id conflict: " + a.getId());
            return;
        }

        boolean nameConflictAfter = accounts.values().stream()
                .filter(acc -> !acc.getId().equals(a.getId()))
                .anyMatch(acc -> acc.getName() != null && acc.getName().trim().toLowerCase().equals(nameKey));
        if (nameConflictAfter) {
            accounts.remove(a.getId(), a);
            System.out.println("Cannot create account: owner name was taken concurrently: " + name);
            return;
        }

        a.addObserver(emailNotifier);
        a.addObserver(smsNotifier);

        System.out.println("Created account: " + a.getId() + " (" + a.getName() + ")");
    }

    private void cmdListAccounts() {
        // accounts table
        if (accounts == null || accounts.isEmpty()) {
            System.out.println("No accounts.");
        } else {
            System.out.println("Accounts:");
            System.out.printf("%-8s %-20s %-12s %-10s%n", "ID", "Owner", "Balance", "Status");
            accounts.values().stream()
                    .sorted(Comparator.comparing(Account::getId))
                    .forEach(a -> {
                        System.out.printf("%-8s %-20s %-12.2f %-10s%n",
                                a.getId(),
                                a.getName(),
                                a.getBalance(),
                                a.getStatusName());
                    });
        }

        System.out.println(); // separator

        // groups summary
        if (groups == null || groups.isEmpty()) {
            System.out.println("Groups: (none)");
            return;
        }

        System.out.println("Groups:");
        // sort by group id for stable output
        // عندما تطبعين المجموعات:
        groups.keySet().stream().sorted().forEach(gid -> {
            accounts.AccountGroup ag = groups.get(gid);
            List<Account> members = ag.getChildren();
            System.out.printf("- Group %s : %d member(s)%n", gid, members.size());
            members.forEach(a -> System.out.printf("    - %-6s owner=%-16s bal=%8.2f status=%s%n",
                    a.getId(), a.getName(), a.getBalance(), a.getStatusName()));
        });


    }

    private Account pickAccount(String prompt) {
        System.out.print(prompt + " (enter id): ");
        String id = scanner.nextLine().trim();
        Account a = accounts.get(id);
        if (a == null) throw new IllegalArgumentException("No account with id: " + id);
        return a;
    }

    private void cmdDeposit(String userId) {
        try {
            Account to = pickAccount("Deposit to");
            System.out.print("Amount: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());
            Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, to, amt);
            boolean ok = facade.deposit(userId, tx);
            System.out.println(ok ? "Deposit processed." : "Deposit failed.");
        } catch (Exception e) {
            System.out.println("Deposit error: " + e.getMessage());
        }
    }

    private void cmdWithdraw(String userId) {
        try {
            Account from = pickAccount("Withdraw from");
            System.out.print("Amount: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());
            Transaction tx = new Transaction(Transaction.Type.WITHDRAW, from, null, amt);
            boolean ok = facade.transfer(userId, tx);
            System.out.println(ok ? "Withdrawal processed." : "Withdrawal failed.");
        } catch (Exception e) {
            System.out.println("Withdraw error: " + e.getMessage());
        }
    }

    private void cmdTransfer(String userId) {
        try {
            System.out.println("Transfer - choose source and destination by id");
            Account from = pickAccount("From");
            Account to = pickAccount("To");
            System.out.print("Amount: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());
            Transaction tx = new Transaction(Transaction.Type.TRANSFER, from, to, amt);
            boolean ok = facade.transfer(userId, tx);
            System.out.println(ok ? "Transfer processed." : "Transfer failed.");
        } catch (Exception e) {
            System.out.println("Transfer error: " + e.getMessage());
        }
    }

    private void cmdScheduleRecurring(String userId) {
        try {
            System.out.println("Schedule recurring - choose accounts");
            Account from = pickAccount("From");
            Account to = pickAccount("To");
            System.out.print("Amount per run: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Initial delay (seconds): ");
            long initial = Long.parseLong(scanner.nextLine().trim());
            System.out.print("Period (seconds): ");
            long period = Long.parseLong(scanner.nextLine().trim());

            RecurringTransaction rtx = new RecurringTransaction(Transaction.Type.TRANSFER, from, to, amt);
            facade.scheduleRecurring(userId, rtx, initial, period);
            System.out.println("Scheduled recurring transaction.");
        } catch (Exception e) {
            System.out.println("Schedule error: " + e.getMessage());
        }
    }

    private void cmdViewHistory() {
        System.out.println("Transaction history:");
        List<Transaction> history = txService.getHistory();
        if (history == null || history.isEmpty()) {
            System.out.println(" - none -");
            return;
        }
        for (Transaction t : history) {
            System.out.printf("- %s %s -> %s : %.2f%n", t.getType(),
                    t.getFrom() != null ? t.getFrom().getId() : "external",
                    t.getTo() != null ? t.getTo().getId() : "external",
                    t.getAmount());
        }
    }

    private void cmdCreateTicket(String currentUserId) {
        try {
            System.out.print("Account id (the card will be issued for this account): ");
            String accountId = scanner.nextLine().trim();
            System.out.print("Ticket subject (e.g. Card request): ");
            String subj = scanner.nextLine().trim();
            System.out.print("Ticket description / notes: ");
            String desc = scanner.nextLine().trim();

            ticketService.create(currentUserId, accountId, subj, desc);
            System.out.println("Ticket created (card request). Admin will review it.");
        } catch (Exception e) {
            System.out.println("Ticket error: " + e.getMessage());
        }
    }

    private void cmdDecorateAccount() {
        try {
            System.out.println("Decorators: 1) OverdraftProtection  2) Insurance");
            System.out.print("Choose decorator: ");
            String dec = scanner.nextLine().trim();
            Account target = pickAccount("Target account id");
            switch (dec) {
                case "1":
                    System.out.print("Extra overdraft limit (positive number): ");
                    double extra = Double.parseDouble(scanner.nextLine().trim());
                    Account wrapped1 = new OverdraftProtectionDecorator(target, extra);
                    accounts.put(wrapped1.getId(), wrapped1);
                    System.out.println("Applied OverdraftProtection to " + wrapped1.getId());
                    break;
                case "2":
                    System.out.print("Insurance cover amount: ");
                    double cover = Double.parseDouble(scanner.nextLine().trim());
                    Account wrapped2 = new InsuranceDecorator(target, cover);
                    accounts.put(wrapped2.getId(), wrapped2);
                    System.out.println("Applied Insurance to " + wrapped2.getId());
                    break;
                default:
                    System.out.println("Unknown decorator");
            }
        } catch (Exception e) {
            System.out.println("Decorator error: " + e.getMessage());
        }
    }

    // -------------------------
    // Group operations (all accept userId for auth checks)
    // -------------------------

    // 10) Create group
    private void cmdCreateAccountGroup(String userId) {
        System.out.print("Create group - enter group name/label: ");
        String label = scanner.nextLine().trim();
        if (label.isEmpty()) { System.out.println("Group name required."); return; }
        String gid = groupService.createGroup(label);
        System.out.println("Group created: " + gid + " (" + label + ")");
    }



    // 11) Add account to group
    private void cmdAddAccountToGroup(String userId) {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        System.out.print("Account id to add: ");
        String aid = scanner.nextLine().trim();

        Account a = accounts.get(aid);
        if (a == null) { System.out.println("No account with id: " + aid); return; }
        if (!"ACTIVE".equalsIgnoreCase(a.getStatusName())) {
            System.out.println("Cannot add account: status=" + a.getStatusName() + " (only ACTIVE allowed).");
            return;
        }
        boolean ok = groupService.addToGroup(gid, aid);
        System.out.println(ok ? "Added account " + aid + " to group " + gid : "Failed to add (group or account unknown).");
    }


    // 12) Remove account from group
    private void cmdRemoveAccountFromGroup(String userId) {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        var og = groupService.getGroup(gid);
        if (og.isEmpty()) { System.out.println("Unknown group: " + gid); return; }
        accounts.AccountGroup ag = og.get();

        System.out.print("Account id to remove: ");
        String aid = scanner.nextLine().trim();

        // find the account object among children
        Account child = ag.getChildren().stream()
                .filter(c -> aid.equals(c.getId()))
                .findFirst()
                .orElse(null);

        if (child == null) {
            System.out.println("Account not found in group.");
            return;
        }

        // remove from group
        ag.remove(child);

        // detach notifiers we attached on add (optional / defensive)
        try {
            child.removeObserver(emailNotifier);
            child.removeObserver(smsNotifier);
        } catch (Exception ignored) {}

        System.out.println("Removed account " + aid + " from group " + gid);
    }

    // 13) Deposit to group — use strategy but call facade for each child (keeps auth/audit)
    private void cmdDepositToGroup(String userId) {
        try {
            System.out.print("Group id: ");
            String gid = scanner.nextLine().trim();
            var og = groupService.getGroup(gid);
            if (og.isEmpty()) { System.out.println("Group not found: " + gid); return; }
            accounts.AccountGroup ag = og.get();

            System.out.print("Total amount to deposit: ");
            double total = Double.parseDouble(scanner.nextLine().trim());
            if (total <= 0) { System.out.println("Amount must be > 0"); return; }

            List<Account> children = ag.getChildren();
            if (children.isEmpty()) { System.out.println("Group has no children."); return; }

            // الحساب يتم عبر الاستراتيجية داخل الـgroup — نحصل على الخطة
            Map<Account, Double> plan = ag.getDepositStrategy().splitDeposit(children, total);

            int success = 0, skippedStatus = 0, failedFacade = 0;
            for (Map.Entry<Account, Double> e : plan.entrySet()) {
                Account a = e.getKey();
                double amt = e.getValue();
                if (amt <= 0) continue;

                // STATUS check
                String status = a.getStatusName();
                if ("CLOSED".equalsIgnoreCase(status) || "SUSPENDED".equalsIgnoreCase(status)) {
                    System.out.printf("Skipping %s: destination account status=%s => cannot deposit%n", a.getId(), status);
                    skippedStatus++;
                    continue;
                }

                Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, a, amt);
                boolean ok = facade.deposit(userId, tx); // facade سيؤدي الفحص والـaudit
                if (!ok) {
                    System.out.println("Deposit failed for " + a.getId() + " (facade rejected).");
                    failedFacade++;
                    continue;
                }
                success++;
                System.out.printf("Deposited %.2f to %s%n", amt, a.getId());
            }

            System.out.printf("Result: deposited to %d/%d members in group %s. Skipped-status=%d, failed-facade=%d%n",
                    success, children.size(), gid, skippedStatus, failedFacade);

        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        } catch (Exception ex) {
            System.out.println("Deposit-to-group error: " + ex.getMessage());
        }
    }


    // 14) Withdraw from group — use strategy then call facade.transfer for each planned withdrawal
    private void cmdWithdrawFromGroup(String userId) {
        try {
            System.out.print("Group id: ");
            String gid = scanner.nextLine().trim();
            var og = groupService.getGroup(gid);
            if (og.isEmpty()) { System.out.println("Unknown or empty group: " + gid); return; }
            accounts.AccountGroup ag = og.get();

            System.out.print("Total amount to withdraw: ");
            double total = Double.parseDouble(scanner.nextLine().trim());
            if (total <= 0) { System.out.println("Amount must be > 0"); return; }

            List<Account> children = ag.getChildren();
            if (children.isEmpty()) { System.out.println("Group has no children."); return; }

            // compute withdraw plan using group's withdraw strategy
            Map<Account, Double> plan;
            try {
                plan = ag.getWithdrawStrategy().splitWithdraw(children, total);
            } catch (IllegalStateException ise) {
                System.out.println("Withdraw plan couldn't be created: " + ise.getMessage());
                return;
            }

            int success = 0, skippedStatus = 0, failedFacade = 0;
            for (Map.Entry<Account, Double> e : plan.entrySet()) {
                Account a = e.getKey();
                double amt = e.getValue();
                if (amt <= 0) continue;

                // only ACTIVE accounts allowed to be debited (policy)
                if (!"ACTIVE".equalsIgnoreCase(a.getStatusName())) {
                    System.out.printf("Skipping %s: cannot withdraw, status=%s%n", a.getId(), a.getStatusName());
                    skippedStatus++;
                    continue;
                }

                Transaction tx = new Transaction(Transaction.Type.WITHDRAW, a, null, amt);
                boolean ok = facade.transfer(userId, tx); // facade = auth + validation + process
                if (!ok) {
                    System.out.println("Withdraw failed for " + a.getId() + " (facade rejected).");
                    failedFacade++;
                    continue;
                }
                success++;
                System.out.printf("Withdrew %.2f from %s%n", amt, a.getId());
            }

            System.out.printf("Result: withdrew from %d/%d members in group %s. Skipped-status=%d, failed-facade=%d%n",
                    success, children.size(), gid, skippedStatus, failedFacade);

        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        } catch (Exception ex) {
            System.out.println("Withdraw-from-group error: " + ex.getMessage());
        }
    }


    // 15) Apply interest to group — iterate children (use facade for deposits so audit/auth preserved)
    private void cmdApplyInterestToGroup(String userId) {
        try {
            System.out.print("Group id: ");
            String gid = scanner.nextLine().trim();
            var og = groupService.getGroup(gid);
            if (og.isEmpty()) { System.out.println("Unknown or empty group: " + gid); return; }
            accounts.AccountGroup ag = og.get();

            System.out.print("Interest percent to apply (e.g. 1.5 for 1.5%): ");
            double pct = Double.parseDouble(scanner.nextLine().trim());
            if (pct <= 0) { System.out.println("Percent must be > 0"); return; }

            int applied = 0;
            for (Account a : ag.getChildren()) {
                if (a == null) continue;
                if (!"ACTIVE".equalsIgnoreCase(a.getStatusName())) continue;
                double add = Math.round(a.getBalance() * (pct / 100.0) * 100.0) / 100.0;
                if (add <= 0) continue;
                Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, a, add);
                boolean ok = facade.deposit(userId, tx);
                if (ok) {
                    applied++;
                    System.out.printf("Applied %.2f to %s%n", add, a.getId());
                }
            }
            System.out.println("Applied interest to " + applied + " children where applicable.");
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        } catch (Exception ex) {
            System.out.println("Apply interest error: " + ex.getMessage());
        }
    }


    /* -------------------------
           Admin dashboard (only ADMIN role)
           - freeze / suspend / close / reopen accounts
           - view account list & some admin actions
           ------------------------- */
    private void cmdAdminDashboard(String userId) {
        if (!auth.authorize(userId, Role.ADMIN)) {
            System.out.println("Unauthorized: ADMIN role required.");
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("=== ADMIN DASHBOARD ===");
            System.out.println("1) List accounts");
            System.out.println("2) Change account status (freeze/suspend/close/reopen)");
            System.out.println("3) View audit (prints audit summary)");
            System.out.println("4) Export audit CSV");
            System.out.println("5) Card management (issue/block/unblock/cancel/list)");
            System.out.println("0) Back");
            System.out.print("> ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    cmdListAccounts();
                    break;
                case "2":
                    cmdChangeAccountStatus();
                    break;
                case "3":
                    cmdPrintAuditSummary();
                    break;
                case "4":
                    cmdExportAuditCsv();
                    break;
                case "5":
                    cmdListTickets(); break;

                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice");
                    break;
            }
        }
    }

    private void cmdChangeAccountStatus() {
        try {
            Account a = pickAccount("Account id");
            System.out.printf("Current status: %s%n", a.getStatusName());
            System.out.println("Choose new status: 1) Freeze 2) Suspend 3) Close 4) Reopen");
            System.out.print("> ");
            String s = scanner.nextLine().trim();
            switch (s) {
                case "1":
                    a.freeze();
                    break;
                case "2":
                    a.suspend();
                    break;
                case "3":
                    a.close();
                    break;
                case "4":
                    a.reopen();
                    break;
                default:
                    System.out.println("Unknown option");
                    return;
            }
            // notify and audit-like print
            try {
                a.notifyObservers("status_change", "Status changed to " + a.getStatusName());
            } catch (Exception ignored) {
            }
            System.out.println("Status changed -> " + a.getStatusName());
        } catch (Exception e) {
            System.out.println("Change status error: " + e.getMessage());
        }
    }

    private void cmdPrintAuditSummary() {
        try {
            System.out.println("=== DASHBOARD SUMMARY ===");
            System.out.println("Transactions total: " + txService.getHistory().size());
            AuditLog audit = txService.getAuditLog();
            if (audit == null) { System.out.println("Audit log not available."); return; }
            System.out.println("Audit entries: " + audit.entriesCount());
            System.out.println("=== Daily Audit Log ===");
            audit.printRecent(10);
        } catch (Exception e) {
            System.out.println("Audit error: " + e.getMessage());
        }
    }



    /* -------------------------
       External transfer (demo)
       ------------------------- */
    private void cmdExternalTransfer(String userId) {
        try {
            System.out.println("External Transfer - choose source (internal account) and external destination (IBAN/email/etc.)");
            Account from = pickAccount("From (internal id)");
            // validate source status: no outgoing if FROZEN, SUSPENDED or CLOSED
            String s = from.getStatusName();
            if ("FROZEN".equalsIgnoreCase(s) || "SUSPENDED".equalsIgnoreCase(s) || "CLOSED".equalsIgnoreCase(s)) {
                System.out.println("Cannot send external transfer: source account status=" + s);
                return;
            }

            System.out.print("To (external identifier, e.g. IBAN/email): ");
            String toInput = scanner.nextLine().trim();
            System.out.print("Amount: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());
            if (amt <= 0) { System.out.println("Amount must be > 0"); return; }

            // check balance
            if (from.getBalance() < amt) {
                System.out.println("Insufficient funds for external transfer. Available: " + from.getBalance());
                return;
            }

            System.out.println("Method: 1) PayPal  2) SWIFT");
            System.out.print("Choose method (1/2): ");
            String m = scanner.nextLine().trim();

            // 1) withdraw internally first (go through facade => auth + txService)
            Transaction withdrawTx = new Transaction(Transaction.Type.WITHDRAW, from, null, amt);
            boolean wOk = facade.transfer(userId, withdrawTx);
            if (!wOk) {
                System.out.println("Internal withdrawal failed. Aborting external transfer.");
                return;
            }

            // 2) Build external transaction (from -> external wrapper) for audit and gateway
            ExternalAccount toWrapper = new ExternalAccount(toInput, toInput);
            Transaction externalTx = new Transaction(Transaction.Type.TRANSFER, from, toWrapper, amt);

            // 3) Record scheduled state in audit and notify owner
            txService.getAuditLog().record(externalTx, "EXTERNAL_SCHEDULED");
            try { from.notifyObservers("external", "External transfer scheduled to " + toInput + " amount " + amt); } catch (Exception ignored) {}

            // 4) choose gateway adapter instance (you may want to centralize this)
            PaymentGateway gateway = ("2".equals(m)) ? new SWIFTAdapter(new SWIFTApi()) : new PayPalAdapter(new PayPalApi());
            // If paymentService was configured for a specific gateway at startup, you could route there.
            // For demo we use existing paymentService which was created with a gateway. If you need per-method gateway,
            // create a lightweight PaymentService wrapper or expose an API to select gateway. Here we'll call paymentService.async.

            // call async transfer (paymentService built with a gateway earlier)
            CompletableFuture<Boolean> fut = paymentService.processExternalTransferAsync(externalTx);

            // 5) callback: on success mark EXTERNAL_EXECUTED; on failure try refund and mark EXTERNAL_FAILED
            fut.whenComplete((success, ex) -> {
                if (ex != null) {
                    // gateway threw exception
                    txService.getAuditLog().record(externalTx, "EXTERNAL_FAILED: " + ex.getMessage());
                    System.out.println("[Async] External transfer error: " + ex.getMessage());
                    // attempt refund
                    try {
                        Transaction refund = new Transaction(Transaction.Type.DEPOSIT, null, from, amt);
                        txService.process(refund);
                        txService.getAuditLog().record(externalTx, "REFUNDED_AFTER_FAILURE");
                    } catch (Exception refundEx) {
                        txService.getAuditLog().record(externalTx, "REFUND_FAILED: " + refundEx.getMessage());
                    }
                    return;
                }

                if (Boolean.TRUE.equals(success)) {
                    txService.getAuditLog().record(externalTx, "EXTERNAL_EXECUTED");
                    try { from.notifyObservers("external", "External transfer to " + toInput + " completed."); } catch (Exception ignored) {}
                } else {
                    txService.getAuditLog().record(externalTx, "EXTERNAL_FAILED");
                    // refund
                    try {
                        Transaction refund = new Transaction(Transaction.Type.DEPOSIT, null, from, amt);
                        txService.process(refund);
                        txService.getAuditLog().record(externalTx, "REFUNDED_AFTER_FAILURE");
                    } catch (Exception refundEx) {
                        txService.getAuditLog().record(externalTx, "REFUND_FAILED: " + refundEx.getMessage());
                    }
                }
            });

            System.out.println("External transfer scheduled (async). You'll be notified when it completes.");
        } catch (Exception e) {
            System.out.println("External transfer error: " + e.getMessage());
        }
    }
    private void cmdExportAuditCsv() {
        try {
            transactions.AuditLog audit = txService.getAuditLog();
            if (audit == null) {
                System.out.println("Audit log not available.");
                return;
            }

            List<transactions.AuditLog.Entry> entries = audit.getEntries();
            if (entries == null || entries.isEmpty()) {
                System.out.println("No audit entries to export.");
                return;
            }

            Path dir = Paths.get("reports");
            Files.createDirectories(dir);

            String fname = "audit-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .format(LocalDateTime.now()) + ".csv";
            Path out = dir.resolve(fname);

            try (BufferedWriter w = Files.newBufferedWriter(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                w.write("timestamp,action,from,to,amount,note");
                w.newLine();
                for (transactions.AuditLog.Entry e : entries) {
                    String ts = e.getTimestamp() != null ? e.getTimestamp().toString() : "";
                    String action = safe(e.getAction());
                    String from = safe(e.getFrom());
                    String to = safe(e.getTo());
                    String amount = String.format("%.2f", e.getAmount());
                    String note = safe(e.getNote());

                    // escape quotes and wrap fields that may contain comma
                    w.write(escapeCsv(ts) + "," + escapeCsv(action) + "," + escapeCsv(from) + "," + escapeCsv(to) + "," + amount + "," + escapeCsv(note));
                    w.newLine();
                }
            }

            System.out.println("Exported audit CSV to: " + out.toAbsolutePath());
        } catch (Exception ex) {
            System.out.println("Export failed: " + ex.getMessage());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n") || out.contains("\r")) {
            return "\"" + out + "\"";
        }
        return out;
    }

    private void cmdListTickets() {
        System.out.println("=== Card Requests (Tickets) ===");
        var list = ticketService.listOpen();
        if (list.isEmpty()) { System.out.println("(no open tickets)"); return; }

        for (Ticket t : list) {
            System.out.printf("%s : account=%s user=%s status=%s created=%s%n  subject=%s%n  desc=%s%n  messages=%s%n",
                    t.id, t.accountId, t.userId, t.status, t.createdAt, t.subject, t.description, t.messages);
        }

        while (true) {
            System.out.println("Options: A <id> = Approve, R <id> = Reject, B = Back");
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toUpperCase();
            if ("B".equals(cmd)) return;

            if (("A".equals(cmd) || "R".equals(cmd)) && parts.length < 2) {
                System.out.println("Missing ticket id. Usage: A <id>  or  R <id>");
                continue;
            }
            String id = parts.length > 1 ? parts[1].trim() : "";

            try {
                if ("A".equals(cmd)) {
                    Card c = ticketService.approve(id, ADMIN_USER);
                    System.out.println("Approved. Issued card id=" + c.getId() + " number=" + c.getMaskedPan());
                    return;
                } else if ("R".equals(cmd)) {
                    System.out.print("Rejection reason: ");
                    String reason = scanner.nextLine().trim();
                    ticketService.reject(id, ADMIN_USER, reason);
                    System.out.println("Rejected ticket " + id);
                    return;
                } else {
                    System.out.println("Unknown command. Use A, R or B.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }


}

