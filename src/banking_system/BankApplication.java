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
import java.util.concurrent.ScheduledFuture;

/**
 * Updated BankApplication that sets up the system and launches the interactive CLI.
 */
public class BankApplication {
    public static void main(String[] args) throws Exception {
        // ---------- Notifiers ----------
        EmailNotifier emailNotifier = new EmailNotifier("ops@bank.com");
        SMSNotifier smsNotifier = new SMSNotifier("+12345");

        // ---------- Approval chain (Chain of Responsibility) ----------
        TransactionValidationHandler validation = new TransactionValidationHandler();
        AutoApprovalHandler auto = new AutoApprovalHandler(500);
        ManagerApprovalHandler manager = new ManagerApprovalHandler(2000);
        validation.setSuccessor(auto);
        auto.setSuccessor(manager);

        // ---------- TransactionService (with chain) ----------
        TransactionService txService = new TransactionService(validation);

        // ---------- Payment (Adapter) ----------
        PayPalApi ppApi = new PayPalApi();
        PaymentGateway ppGateway = new PayPalAdapter(ppApi);
        PaymentService paymentService = new PaymentService(ppGateway);

        // ---------- AuthService (RBAC) ----------
        AuthService auth = new AuthService();
        auth.register("user1", Role.CUSTOMER);
        auth.register("mgr1", Role.MANAGER);
        // You can register more users and roles as needed

        // ---------- Facade ----------
        BankingFacade facade = new BankingFacade(txService, auth, paymentService);

        // ---------- Ticket service & recommendations & admin ----------
        TicketService ticketService = new TicketService();
        RecommendationService recommendationService = new RecommendationService();

        // ---------- Put accounts into a map for the InteractiveConsole ----------
        Map<String, Account> accountsMap = new LinkedHashMap<>();

        // ---------- Start Interactive CLI ----------
        // NOTE: InteractiveConsole constructor must accept PaymentService as the last argument.
        InteractiveConsole console = new InteractiveConsole(accountsMap, txService, facade, auth, ticketService, paymentService);
        console.start();

        // ---------- After CLI returns, optionally show summary and shutdown ----------
        System.out.println("Shutting down services...");

        // Print dashboard & reports (optional)
        DashboardService dashboard = new DashboardService(txService.getHistory(), txService.getAuditLog());
        dashboard.printSummary();
        ReportingService reporting = new ReportingService(txService.getAuditLog());
        reporting.dailyReport();

        // If any recurring jobs were scheduled, shut down the txService scheduler
        txService.shutdown();

        System.out.println("Application stopped.");
    }
}
