package banking_system;

import accounts.*;
import accounts.decorators.InsuranceDecorator;
import accounts.decorators.OverdraftProtectionDecorator;
import accounts.factory.AccountFactory;
import accounts.state.ActiveState;
import accounts.state.AccountStatus;
import customers.TicketService;
import notifications.EmailNotifier;
import notifications.NotificationObserver;
import notifications.SMSNotifier;
import payment.*;
import recommendations.RecommendationService;
import security.AuthService;
import security.Role;
import transactions.RecurringTransaction;
import transactions.Transaction;
import transactions.TransactionService;

import java.util.*;

/**
 * Interactive CLI for the banking system.
 * - added External transfer option (Adapter usage)
 */
public class InteractiveConsole {
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Account> accounts; // id -> Account
    private final TransactionService txService;
    private final banking_system.BankingFacade facade;
    private final AuthService auth;
    private final TicketService ticketService;
    private final PaymentService paymentService;
    // helpful notifiers
    private final EmailNotifier emailNotifier = new EmailNotifier("ops@bank.com");
    private final SMSNotifier smsNotifier = new SMSNotifier("+12345");

    public InteractiveConsole(Map<String, Account> accounts,
                              TransactionService txService,
                              BankingFacade facade,
                              AuthService auth,
                              TicketService ticketService,
                              PaymentService paymentService) {
        this.accounts = accounts;
        this.txService = txService;
        this.facade = facade;
        this.auth = auth;
        this.ticketService = ticketService;
        this.paymentService = paymentService;
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
                    case "10": cmdCreateAccountGroup(); break;       // if you have groups
                    case "11": cmdAddAccountToGroup(); break;
                    case "12": cmdRemoveAccountFromGroup(); break;
                    case "13": cmdDepositToGroup(); break;
                    case "14": cmdWithdrawFromGroup(); break;
                    case "15": cmdApplyInterestToGroup(); break;
                    case "16": cmdExternalTransfer(userId); break;   // <-- NEW OPTION
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
        System.out.println("16) External transfer (via gateway)");
        System.out.println("0) Exit");
        System.out.print("> ");
    }

    /* -------------------------
       Account creation (safe)
       ------------------------- */
    private void cmdCreateAccount(String currentUserId) {
        System.out.println("Choose type: 1) Savings 2) Checking 3) Loan 4) Investment");
        String t = scanner.nextLine().trim();

        // always auto-generate id (factory handles null)
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

    /* -------------------------
       List accounts
       ------------------------- */
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

    /* -------------------------
       Helper: pick account by id (throws if missing)
       ------------------------- */
    private Account pickAccount(String prompt) {
        System.out.print(prompt + " (enter id): ");
        String id = scanner.nextLine().trim();
        Account a = accounts.get(id);
        if (a == null) throw new IllegalArgumentException("No account with id: " + id);
        return a;
    }

    /* -------------------------
       Deposit / Withdraw / Transfer
       ------------------------- */
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
            boolean ok = facade.transfer(userId, tx); // uses same auth/processing path
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
        if (history.isEmpty()) System.out.println(" - none -");
        for (Transaction t : history) {
            System.out.printf("- %s %s -> %s : %.2f%n", t.getType(),
                    t.getFrom() != null ? t.getFrom().getId() : "external",
                    t.getTo() != null ? t.getTo().getId() : "external",
                    t.getAmount());
        }
    }

    private void cmdCreateTicket(String userId) {
        try {
            System.out.print("Ticket subject: ");
            String subj = scanner.nextLine().trim();
            System.out.print("Ticket description: ");
            String desc = scanner.nextLine().trim();
            ticketService.create(userId, subj, desc);
            System.out.println("Ticket created.");
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
        } catch (Exception e) {
            System.out.println("Decorator error: " + e.getMessage());
        }
    }

    /* -------------------------
       Group-related stubs (implement if you have AccountGroup API)
       ------------------------- */
    private void cmdCreateAccountGroup() { System.out.println("Create group - not implemented in this snippet"); }
    private void cmdAddAccountToGroup() { System.out.println("Add to group - not implemented in this snippet"); }
    private void cmdRemoveAccountFromGroup() { System.out.println("Remove from group - not implemented in this snippet"); }
    private void cmdDepositToGroup() { System.out.println("Deposit to group - not implemented in this snippet"); }
    private void cmdWithdrawFromGroup() { System.out.println("Withdraw from group - not implemented in this snippet"); }
    private void cmdApplyInterestToGroup() { System.out.println("Apply interest to group - not implemented in this snippet"); }

    /* -------------------------
       NEW: External transfer via adapter (PayPal / SWIFT)
       ------------------------- */// داخل InteractiveConsole
    private void cmdExternalTransfer(String userId) {
        try {
            System.out.println("External Transfer - choose source (internal account) and external destination (IBAN/email/etc.)");
            Account from = pickAccount("From (internal id)");
            System.out.print("To (external identifier, e.g. IBAN/email): ");
            String toInput = scanner.nextLine().trim();
            if (toInput.isEmpty()) {
                System.out.println("Destination required.");
                return;
            }

            System.out.print("Amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim());
            if (amount <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }

            System.out.println("Method: 1) PayPal  2) SWIFT");
            String method = scanner.nextLine().trim();
            // For external transfers we will create a lightweight external Account wrapper so adapters
            // can receive an Account object if your adapters expect Account.getId() etc.
            Account external = new payment.ExternalAccount(toInput, toInput); // make sure ExternalAccount ctor is public

            // Create transaction (from internal -> external)
            Transaction tx = new Transaction(Transaction.Type.TRANSFER, from, external, amount);

            // IMPORTANT: do NOT bypass facade / txService. Use facade.transfer so we get:
            //  - authorization check
            //  - pre-withdraw (reserve) and rollback logic (implemented in BankingFacade)
            //  - external gateway invocation (PaymentService) and audit
            // (The BankingFacade already uses paymentService for large/external amounts)
            boolean ok = facade.transfer(userId, tx);
            System.out.println(ok ? "External transfer completed successfully." : "External transfer FAILED.");
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid number: " + nfe.getMessage());
        } catch (Exception e) {
            System.out.println("External transfer error: " + e.getMessage());
        }
    }

}
