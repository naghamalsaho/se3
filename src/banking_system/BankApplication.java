package banking_system;

import accounts.*;

import notifications.EmailNotifier;
import notifications.SMSNotifier;
import payment.PayPalAdapter;
import payment.PayPalApi;
import payment.PaymentGateway;
import payment.PaymentService;
import transactions.*;
import security.AuthService;
import security.Role;

import customers.Ticket;
import customers.TicketService;
import admin.DashboardService;
import admin.ReportingService;
import recommendations.RecommendationService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

/**
 * Updated BankApplication that sets up the system and launches the interactive CLI.
 */
public class BankApplication {
    public static void main(String[] args) throws Exception {
        // ---------- Notifiers ----------
        EmailNotifier emailNotifier = new EmailNotifier("ops@bank.com");
        SMSNotifier smsNotifier = new SMSNotifier("+12345");

        // ---------- Approval chain ----------
        TransactionValidationHandler validation = new TransactionValidationHandler();
        AutoApprovalHandler auto = new AutoApprovalHandler(500);
        ManagerApprovalHandler manager = new ManagerApprovalHandler(2000);
        validation.setSuccessor(auto);
        auto.setSuccessor(manager);

        // ---------- TransactionService ----------
        TransactionService txService = new TransactionService(validation);

        // ---------- Payment (Adapter) ----------
        PayPalApi ppApi = new PayPalApi();
        PaymentGateway ppGateway = new PayPalAdapter(ppApi);

        // create a dedicated executor for gateway calls (tunable)
        int cpus = Runtime.getRuntime().availableProcessors();
        // use fixed pool size (example):  max(4, cpus*2) or tune as needed
        ExecutorService gatewayExecutor = Executors.newFixedThreadPool(Math.max(4, cpus * 2));

        PaymentService paymentService = new PaymentService(ppGateway, gatewayExecutor);

        // ---------- AuthService ----------
        AuthService auth = new AuthService();
        auth.register("user1", Role.CUSTOMER);
        auth.register("mgr1", Role.MANAGER);

        // ---------- Facade ----------
        BankingFacade facade = new BankingFacade(txService, auth, paymentService);

        // ---------- Ticket service ----------
        TicketService ticketService = new TicketService();

        // ---------- Put accounts into a map for the InteractiveConsole ----------
        Map<String, Account> accountsMap = new LinkedHashMap<>();

        // ---------- Start Interactive CLI ----------
        InteractiveConsole console = new InteractiveConsole(accountsMap, txService, facade, auth, ticketService, paymentService);
        console.start();

        // ---------- Shutdown ----------
        System.out.println("Shutting down services...");
        txService.shutdown();
        paymentService.shutdownExecutor();
        System.out.println("Application stopped.");
    }
}