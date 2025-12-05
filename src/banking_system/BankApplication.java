package banking_system;

import accounts.*;
import accounts.factory.AccountFactory;
import notifications.*;
import transactions.*;
import security.*;
import customers.*;
import admin.*;
import recommendations.*;

import java.util.concurrent.ScheduledFuture;
import java.util.List;

public class BankApplication {
    public static void main(String[] args) throws Exception {
        // setup accounts
        SavingsAccount s = (SavingsAccount) AccountFactory.createSavings("s1","Alice",10000);
        CheckingAccount c = (CheckingAccount) AccountFactory.createChecking("c1","Bob",500);
        LoanAccount loan = (LoanAccount) AccountFactory.createLoan("l1","AliceLoan",5000,5.0);
        InvestmentAccount inv = (InvestmentAccount) AccountFactory.createInvestment("i1","AliceInvest",15000,"balanced");

        // observers
        EmailNotifier email = new EmailNotifier("ops@bank.com");
        SMSNotifier sms = new SMSNotifier("+12345");
        s.addObserver(email); s.addObserver(sms); c.addObserver(email); loan.addObserver(email); inv.addObserver(email);

        // chain
        TransactionValidationHandler validation = new TransactionValidationHandler();
        AutoApprovalHandler auto = new AutoApprovalHandler(500);
        ManagerApprovalHandler manager = new ManagerApprovalHandler(2000);
        validation.setSuccessor(auto); auto.setSuccessor(manager);

        TransactionService txService = new TransactionService(validation);

        // process a transfer
        Transaction tx1 = new Transaction(Transaction.Type.TRANSFER, s, c, 300);
        txService.process(tx1);

        // schedule recurring loan payment monthly (for demo use seconds)
        RecurringTransaction rtx = new RecurringTransaction(Transaction.Type.DEPOSIT, c, loan, 100);
        ScheduledFuture<?> scheduled = txService.scheduleRecurring(rtx, 2, 5); // initial 2s, every 5s

        // simple ticket
        TicketService ticketService = new TicketService();
        Ticket t = ticketService.create("user1", "App login", "Cannot login");
        ticketService.addMessage(t.id, "Support: reset password");

        // recommendations
        RecommendationService rec = new RecommendationService();
        List<String> recs = rec.analyze(txService.getHistory(), s);
        recs.forEach(System.out::println);

        // auth
        AuthService auth = new AuthService();
        auth.register("user1", Role.CUSTOMER);
        System.out.println("Auth user1 teller? " + auth.authorize("user1", Role.TELLER));

        // dashboard & report
        DashboardService dashboard = new DashboardService(txService.getHistory(), txService.getAuditLog());
        dashboard.printSummary();
        ReportingService report = new ReportingService(txService.getAuditLog());
        report.dailyReport();

        // run 12 seconds demo then shutdown
        Thread.sleep(12000);
        scheduled.cancel(true);
        txService.shutdown();
    }
}
