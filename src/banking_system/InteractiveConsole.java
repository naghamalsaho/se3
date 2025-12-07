package banking_system;

import accounts.*;
import accounts.factory.AccountFactory;
import accounts.decorators.InsuranceDecorator;
import accounts.decorators.OverdraftProtectionDecorator;
import customers.TicketService;
import notifications.EmailNotifier;
import notifications.SMSNotifier;
import accounts.AccountGroup;
import accounts.EvenSplitDeposit;
import accounts.SingleTargetDeposit;
import accounts.SequentialWithdraw;
import interest.SimpleInterestStrategy;
import accounts.SavingsAccount;

import security.AuthService;
import security.Role;
import transactions.RecurringTransaction;
import transactions.Transaction;
import transactions.TransactionService;

import java.util.*;



/**
 * Interactive CLI for the banking system.
 * - create accounts (savings, checking, loan, investment)
 * - list accounts
 * - select accounts by id and do deposit/withdraw/transfer
 * - schedule recurring payments
 * - view history / audit via facade/txService
 *
 * Works with your existing AccountFactory, TransactionService, BankingFacade, etc.
 */
public class InteractiveConsole {
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Account> accounts; // id -> Account
    private final TransactionService txService;
    private final banking_system.BankingFacade facade;
    private final AuthService auth;
    private final TicketService ticketService;

    // helpful notifiers
    private final EmailNotifier emailNotifier = new EmailNotifier("ops@bank.com");
    private final SMSNotifier smsNotifier = new SMSNotifier("+12345");

    public InteractiveConsole(Map<String, Account> accounts,
                              TransactionService txService,
                              banking_system.BankingFacade facade,
                              AuthService auth,
                              TicketService ticketService) {
        this.accounts = accounts;
        this.txService = txService;
        this.facade = facade;
        this.auth = auth;
        this.ticketService = ticketService;
    }

    public void start() {
        System.out.println("=== Welcome to the Interactive Bank CLI ===");
        System.out.print("Login userId (or press Enter to use 'guest'): ");
        String userId = scanner.nextLine().trim();
        if (userId.isEmpty()) userId = "guest";

        // ensure user registered
        if (!auth.authorize(userId, Role.CUSTOMER)) {
            auth.register(userId, Role.CUSTOMER);
            System.out.println("Registered user as CUSTOMER: " + userId);
        }

        // attach notifiers by default to existing accounts
        accounts.values().forEach(a -> { a.addObserver(emailNotifier); a.addObserver(smsNotifier); });

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": cmdCreateAccount(userId); break;
                    case "2": cmdListAccounts(); break;
                    case "3": cmdDeposit(userId); break;
                    case "4": cmdWithdraw(userId); break;
                    case "5": cmdTransfer(userId); break;
                    case "6": cmdScheduleRecurring(userId); break;
                    case "7": cmdViewHistory(); break;
                    case "8": cmdCreateTicket(userId); break;
                    case "9": cmdDecorateAccount(); break;
                    case "10": cmdCreateGroup(); break;
                    case "11": cmdAddToGroup(); break;
                    case "12": cmdRemoveFromGroup(); break;
                    case "13": cmdDepositToGroup(); break;
                    case "14": cmdWithdrawFromGroup(); break;
                    case "15": cmdApplyInterestToGroup(); break;

                    case "0": running = false; break;
                    default: System.out.println("Invalid choice"); break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }

        System.out.println("Exiting CLI. Bye!");
    }

    private void printMainMenu() {
        System.out.println("Menu:");
        System.out.println("1) Create account");
        System.out.println("2) List accounts");
        System.out.println("3) Deposit");
        System.out.println("4) Withdraw");
        System.out.println("5) Transfer");
        System.out.println("6) Schedule recurring payment");
        System.out.println("7) View transaction history");
        System.out.println("8) Create support ticket");
        System.out.println("9) Add feature to account (Decorator)");
        System.out.println("10) Create account group");
        System.out.println("11) Add account to group");
        System.out.println("12) Remove account from group");
                System.out.println("13) Deposit to group");
                        System.out.println("14) Withdraw from group");
                                System.out.println("15) Apply interest to group");

        System.out.println("0) Exit");
        System.out.print("> ");
    }

    private void cmdCreateAccount(String userId) {
        System.out.println("Choose type: 1) Savings 2) Checking 3) Loan 4) Investment");
        String t = scanner.nextLine().trim();

        // no id asked anymore - always auto-generate
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

        // check owner name uniqueness (case-insensitive)
        boolean nameExists = accounts.values().stream()
                .anyMatch(a -> a.getName() != null && a.getName().trim().toLowerCase().equals(nameKey));
        if (nameExists) {
            System.out.println("Cannot create account: owner name already exists: " + name);
            return;
        }

        // create via factory (pass null id to auto-generate)
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

        // try atomic insertion by id to avoid race on generated id
        Account existing = accounts.putIfAbsent(a.getId(), a);
        if (existing != null) {
            System.out.println("Cannot register account: generated id conflict: " + a.getId());
            return;
        }

        // double-check name uniqueness (race-safe): if some other thread added same name meanwhile, rollback
        boolean nameConflictAfter = accounts.values().stream()
                .filter(acc -> !acc.getId().equals(a.getId()))
                .anyMatch(acc -> acc.getName() != null && acc.getName().trim().toLowerCase().equals(nameKey));
        if (nameConflictAfter) {
            // rollback insertion
            accounts.remove(a.getId(), a);
            System.out.println("Cannot create account: owner name was taken concurrently: " + name);
            return;
        }

        // attach observers and finish
        a.addObserver(emailNotifier);
        a.addObserver(smsNotifier);

        System.out.println("Created account: " + a.getId() + " (" + a.getName() + ")");
    }



    private void cmdListAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts.");
            return;
        }
        System.out.println("Accounts:");
        for (Account a : accounts.values()) {
            System.out.printf("- id=%s name=%s balance=%.2f status=%s%n",
                    a.getId(), a.getName(), a.getBalance(), a.getStatusName());
        }
    }

    private Account pickAccount(String prompt) {
        System.out.print(prompt + " (enter id): ");
        String id = scanner.nextLine().trim();
        Account a = accounts.get(id);
        if (a == null) throw new IllegalArgumentException("No account with id: " + id);
        return a;
    }

    private void cmdDeposit(String userId) {
        Account to = pickAccount("Deposit to");
        System.out.print("Amount: ");
        double amt = Double.parseDouble(scanner.nextLine().trim());
        Transaction tx = new Transaction(Transaction.Type.DEPOSIT, null, to, amt);
        boolean ok = facade.deposit(userId, tx);
        System.out.println(ok ? "Deposit processed." : "Deposit failed.");
    }

    private void cmdWithdraw(String userId) {
        Account from = pickAccount("Withdraw from");
        System.out.print("Amount: ");
        double amt = Double.parseDouble(scanner.nextLine().trim());
        Transaction tx = new Transaction(Transaction.Type.WITHDRAW, from, null, amt);
        boolean ok = facade.transfer(userId, tx); // uses same auth/processing path
        System.out.println(ok ? "Withdrawal processed." : "Withdrawal failed.");
    }

    private void cmdTransfer(String userId) {
        System.out.println("Transfer - choose source and destination by id");
        Account from = pickAccount("From");
        Account to = pickAccount("To");
        System.out.print("Amount: ");
        double amt = Double.parseDouble(scanner.nextLine().trim());
        Transaction tx = new Transaction(Transaction.Type.TRANSFER, from, to, amt);
        boolean ok = facade.transfer(userId, tx);
        System.out.println(ok ? "Transfer processed." : "Transfer failed.");
    }

    private void cmdScheduleRecurring(String userId) {
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
    }

    private void cmdViewHistory() {
        System.out.println("Transaction history:");
        List<Transaction> history = txService.getHistory();
        if (history.isEmpty()) System.out.println(" - none -");
        for (Transaction t : history) {
            System.out.printf("- %s %s -> %s : %.2f%n", t.getType(),
                    t.getFrom() != null ? t.getFrom().getId() : "external",
                    t.getTo() != null ? t.getTo().getId() : "external",
                    t.getAmount());
        }
    }

    private void cmdCreateTicket(String userId) {
        System.out.print("Ticket subject: ");
        String subj = scanner.nextLine().trim();
        System.out.print("Ticket description: ");
        String desc = scanner.nextLine().trim();
        ticketService.create(userId, subj, desc);
        System.out.println("Ticket created.");
    }

    private void cmdDecorateAccount() {
        System.out.println("Decorators: 1) OverdraftProtection  2) Insurance");
        System.out.print("Choose decorator: ");
        String dec = scanner.nextLine().trim();
        Account target = pickAccount("Target account id");
        switch (dec) {
            case "1":
                System.out.print("Extra overdraft limit (positive number): ");
                double extra = Double.parseDouble(scanner.nextLine().trim());
                Account wrapped1 = new OverdraftProtectionDecorator(target, extra);
                // replace mapping so future ops use decorated account
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
    }
    // 10) Create account group
    private void cmdCreateGroup() {
        System.out.print("Enter group name: ");
        String gname = scanner.nextLine().trim();
        if (gname.isEmpty()) { System.out.println("Group name required."); return; }
        String gid = "g" + UUID.randomUUID().toString().substring(0,5);
        AccountGroup g = new AccountGroup(gid, gname);
        // attach global notifiers so children will also notify (AccountGroup.addObserver forwards to children)
        g.addObserver(emailNotifier);
        g.addObserver(smsNotifier);
        accounts.put(gid, g);
        System.out.println("Created group: " + gid + " (" + gname + ")");
    }

    // 11) Add account to group
    private void cmdAddToGroup() {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        Account grp = accounts.get(gid);
        if (!(grp instanceof AccountGroup)) { System.out.println("Not a group id."); return; }
        System.out.print("Account id to add: ");
        String cid = scanner.nextLine().trim();
        Account child = accounts.get(cid);
        if (child == null) { System.out.println("No such account: " + cid); return; }
        ((AccountGroup)grp).add(child);
        // ensure child has notifiers too
        child.addObserver(emailNotifier);
        child.addObserver(smsNotifier);
        System.out.println("Added " + cid + " to group " + gid);
    }

    // 12) Remove account from group
    private void cmdRemoveFromGroup() {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        Account grp = accounts.get(gid);
        if (!(grp instanceof AccountGroup)) { System.out.println("Not a group id."); return; }
        System.out.print("Account id to remove: ");
        String cid = scanner.nextLine().trim();
        Account child = accounts.get(cid);
        if (child == null) { System.out.println("No such account: " + cid); return; }
        ((AccountGroup)grp).remove(child);
        System.out.println("Removed " + cid + " from group " + gid);
    }

    // 13) Deposit to group
    private void cmdDepositToGroup() {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        Account grp = accounts.get(gid);
        if (!(grp instanceof AccountGroup)) { System.out.println("Not a group id."); return; }

        System.out.print("Amount: ");
        double amt;
        try { amt = Double.parseDouble(scanner.nextLine().trim()); } catch(NumberFormatException e){ System.out.println("Invalid amount"); return; }

        System.out.println("Deposit policy: 1) Even split  2) To single child");
        String pol = scanner.nextLine().trim();
        AccountGroup g = (AccountGroup) grp;
        if ("1".equals(pol)) {
            g.setDepositStrategy(new EvenSplitDeposit());
        } else if ("2".equals(pol)) {
            System.out.print("Target child id: ");
            String cid = scanner.nextLine().trim();
            Account child = accounts.get(cid);
            if (child == null) { System.out.println("No such child: " + cid); return; }
            g.setDepositStrategy(new SingleTargetDeposit(child));
        } else {
            System.out.println("Unknown policy"); return;
        }
        try {
            g.deposit(amt);
            System.out.println("Group deposit executed.");
        } catch(Exception e){
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    // 14) Withdraw from group
    private void cmdWithdrawFromGroup() {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        Account grp = accounts.get(gid);
        if (!(grp instanceof AccountGroup)) { System.out.println("Not a group id."); return; }

        System.out.print("Amount: ");
        double amt;
        try { amt = Double.parseDouble(scanner.nextLine().trim()); } catch(NumberFormatException e){ System.out.println("Invalid amount"); return; }

        System.out.println("Withdraw policy: 1) Sequential (use balances in order)");
        String pol = scanner.nextLine().trim();
        AccountGroup g = (AccountGroup) grp;
        if ("1".equals(pol)) {
            g.setWithdrawStrategy(new SequentialWithdraw());
        } else {
            System.out.println("Unknown policy"); return;
        }
        try {
            g.withdraw(amt);
            System.out.println("Group withdraw executed.");
        } catch (Exception e) {
            System.out.println("Withdraw failed: " + e.getMessage());
        }
    }

    // 15) Apply interest to group
    private void cmdApplyInterestToGroup() {
        System.out.print("Group id: ");
        String gid = scanner.nextLine().trim();
        Account grp = accounts.get(gid);
        if (!(grp instanceof AccountGroup)) { System.out.println("Not a group id."); return; }

        System.out.print("Months to compute interest for (integer): ");
        int months;
        try { months = Integer.parseInt(scanner.nextLine().trim()); } catch(NumberFormatException e){ System.out.println("Invalid months"); return; }

        AccountGroup g = (AccountGroup) grp;

        // DEBUG: print children and their runtime classes
        System.out.println("Group children (debug):");
        for (Account child : g.getChildren()) {
            System.out.printf(" - id=%s name=%s runtimeClass=%s%n", child.getId(), child.getName(), child.getClass().getName());
        }

        // Strategy: use a configurable strategy (example uses SimpleInterestStrategy 1% yearly)
        interest.InterestStrategy strat = new interest.SimpleInterestStrategy(1.0);
        int applied = 0;

        for (Account child : g.getChildren()) {
            Account core = unwrapDecorators(child); // try to get the wrapped/original account
            boolean appliedThis = false;

            if (core instanceof accounts.SavingsAccount) {
                double interestAmount = strat.computeInterest((accounts.SavingsAccount) core, months);
                if (interestAmount > 0) {
                    // deposit on the actual public API (so observers fire)
                    core.deposit(interestAmount);
                    applied++;
                    appliedThis = true;
                }
            }

            System.out.printf(" -> child %s (%s) => coreClass=%s applied=%s%n",
                    child.getId(), child.getName(), core.getClass().getName(), appliedThis);
        }

        System.out.println("Applied interest to " + applied + " children where applicable.");
    }

    // helper: try to peel decorators to reach underlying account
    private Account unwrapDecorators(Account a) {
        Account cur = a;
        // fast-path if we know AccountDecorator class exists in package accounts.decorators
        try {
            while (cur != null) {
                Class<?> cls = cur.getClass();
                String simple = cls.getSimpleName().toLowerCase();
                // heuristic: decorator classes usually have 'Decorator' in name or are in accounts.decorators package
                if (simple.endsWith("decorator") || cls.getPackageName().contains(".decorators")) {
                    // try typed method getDelegate() or getWrapped() common convention
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getDelegate");
                        Object inner = m.invoke(cur);
                        if (inner instanceof Account) { cur = (Account) inner; continue; }
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        java.lang.reflect.Method m2 = cls.getMethod("getWrapped");
                        Object inner2 = m2.invoke(cur);
                        if (inner2 instanceof Account) { cur = (Account) inner2; continue; }
                    } catch (NoSuchMethodException ignored) {}
                    // if no known getter, break and return current (cannot unwrap)
                    break;
                } else {
                    break; // not a decorator by naming/packaging
                }
            }
        } catch (Exception e) {
            // on any reflection error, return original
            return a;
        }
        return cur;
    }


}