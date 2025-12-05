package accounts.factory;

import accounts.*;

public class AccountFactory {
    public static Account createSavings(String id, String name, double initial){
        return new SavingsAccount(id, name, initial);
    }
    public static Account createChecking(String id, String name, double initial){
        return new CheckingAccount(id, name, initial);
    }
    public static Account createLoan(String id, String name, double principal, double rate){
        return new LoanAccount(id, name, principal, rate);
    }
    public static Account createInvestment(String id, String name, double initial, String type){
        return new InvestmentAccount(id, name, initial, type);
    }
}
