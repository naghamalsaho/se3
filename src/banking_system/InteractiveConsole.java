package banking_system;

import accounts.*;
import accounts.factory.AccountFactory;
import accounts.decorators.InsuranceDecorator;
import accounts.decorators.OverdraftProtectionDecorator;
import customers.TicketService;
import notifications.EmailNotifier;
import notifications.SMSNotifier;
import payment.PayPalApi;
import payment.PayPalAdapter;
import payment.PaymentGateway;
import payment.PaymentService;
import recommendations.RecommendationService;
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
        System.out.println("0) Exit");
        System.out.print("> ");
    }

    private void cmdCreateAccount(String userId) {
        System.out.println("Choose type: 1) Savings 2) Checking 3) Loan 4) Investment");
        String t = scanner.nextLine().trim();
        System.out.print("Enter account id (e.g. s2): ");
        String id = scanner.nextLine().trim();
        System.out.print("Enter account owner/name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Initial amount (number): ");
        double initial = Double.parseDouble(scanner.nextLine().trim());

        Account a;
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
                a = AccountFactory.createInvestment(id, name, initial, p);
                break;
            default:
                System.out.println("Unknown type");
                return;
        }

        // register observer default
        a.addObserver(emailNotifier);
        a.addObserver(smsNotifier);

        accounts.put(a.getId(), a);
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
}
