package accounts.factory;

import accounts.*;
import util.InputValidator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AccountFactory with safe creation helpers and auto-id generation.
 * - if provided id is null/invalid, it generates a safe id.
 */
public final class AccountFactory {
    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private static String nextSuffix(String prefix){
        counters.putIfAbsent(prefix, new AtomicInteger(0));
        int n = counters.get(prefix).incrementAndGet();
        return prefix + n;
    }

    public static String generateIdForType(String type){ // type: savings, checking, loan, investment
        switch(type.toLowerCase()){
            case "savings": return nextSuffix("s");
            case "checking": return nextSuffix("c");
            case "loan": return nextSuffix("l");
            case "investment": return nextSuffix("i");
            case "group": return nextSuffix("g");
            default: return nextSuffix("a");
        }
    }

    public static Account createSavings(String id, String name, double initial){
        String safeId = (id != null && InputValidator.isValidAccountId(id)) ? InputValidator.normalizeAccountId(id) : generateIdForType("savings");
        String safeName = InputValidator.sanitizeName(name);
        return new SavingsAccount(safeId, safeName, initial);
    }

    public static Account createChecking(String id, String name, double initial){
        String safeId = (id != null && InputValidator.isValidAccountId(id)) ? InputValidator.normalizeAccountId(id) : generateIdForType("checking");
        String safeName = InputValidator.sanitizeName(name);
        return new CheckingAccount(safeId, safeName, initial);
    }

    public static Account createLoan(String id, String name, double initial, double yearlyRate){
        String safeId = (id != null && InputValidator.isValidAccountId(id)) ? InputValidator.normalizeAccountId(id) : generateIdForType("loan");
        String safeName = InputValidator.sanitizeName(name);
        return new LoanAccount(safeId, safeName, initial, yearlyRate);
    }

    public static Account createInvestment(String id, String name, double initial, String portfolioType){
        String safeId = (id != null && InputValidator.isValidAccountId(id)) ? InputValidator.normalizeAccountId(id) : generateIdForType("investment");
        String safeName = InputValidator.sanitizeName(name);
        return new InvestmentAccount(safeId, safeName, initial, portfolioType == null ? "balanced" : InputValidator.sanitizeName(portfolioType));
    }

    private AccountFactory(){}
}
