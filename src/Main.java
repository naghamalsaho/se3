////TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
//// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//
//
//import accounts.*;
//import banking_system.BankFacade;
//import interest.*;
//import notifications.*;
//import transactions.*;
//
//public class Main {
//    public static void main(String[] args) {
//        // setup accounts
//        SavingsAccount s1 = new SavingsAccount("s1", "Alice Saving", 1000);
//        CheckingAccount c1 = new CheckingAccount("c1", "Bob Checking", 200);
//
//        // observers
//        EmailNotifier email = new EmailNotifier("ops@bank.com");
//        SMSNotifier sms = new SMSNotifier("+12345678");
//
//        s1.addObserver(email);
//        s1.addObserver(sms);
//        c1.addObserver(email);
//
//        // composite group
//        AccountGroup family = new AccountGroup("g1", "Family Accounts");
//        family.add(s1); family.add(c1);
//
//        System.out.println("Family balance: " + family.getBalance());
//
//        // interest strategy demo
//        InterestStrategy strat = new SimpleInterestStrategy(5.0); // 5% yearly
//        System.out.println("Interest for s1 (6 months): " + strat.computeInterest(s1,6));
//
//        // Setup chain: auto approve <=500 -> manager <=2000
//        // Setup chain: Validation -> auto approve <=500 -> manager <=2000
//        TransactionValidationHandler validation = new TransactionValidationHandler();
//        AutoApprovalHandler auto = new AutoApprovalHandler(500);
//        ManagerApprovalHandler manager = new ManagerApprovalHandler(2000);
//
//        validation.setSuccessor(auto);
//        auto.setSuccessor(manager);
//
//// Use validation as entry point for approvals
//        BankFacade facade = new BankFacade(validation);
//
//
//
//
//        // transfer triggers chain and notifications
//        System.out.println("\n-- Attempt transfer 300 from s1 to c1 --");
//        facade.transfer(s1, c1, 300);
//
//        System.out.println("\n-- Attempt transfer 1500 from s1 to c1 --");
//        facade.transfer(s1, c1, 1500);
//    }
//}
//
