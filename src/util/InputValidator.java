package util;

import java.util.regex.Pattern;

/**
 * Utility for validating and sanitizing user inputs.
 * Simple, dependency-free sanitizer (removes tags).
 */
public final class InputValidator {
    private static final Pattern ACCOUNT_ID = Pattern.compile("^[a-z0-9_-]{2,30}$");
    private static final Pattern NAME_ALLOWED = Pattern.compile("^[\\p{L}0-9 .,'\\-]{1,100}$");

    private InputValidator(){}

    public static boolean isValidAccountId(String id){
        if(id == null) return false;
        String s = id.trim().toLowerCase();
        return ACCOUNT_ID.matcher(s).matches();
    }

    public static String normalizeAccountId(String id){
        if(id == null) return null;
        return id.trim().toLowerCase();
    }

    public static boolean isValidOwnerName(String name){
        if(name == null) return false;
        String s = name.trim();
        return s.length() >= 1 && s.length() <= 100 && NAME_ALLOWED.matcher(s).matches();
    }

    public static String sanitizeName(String name){
        if(name == null) return "";
        String cleaned = name.replaceAll("<[^>]*>", ""); // strip tags
        cleaned = cleaned.replaceAll("[\\r\\n]+", " ").trim(); // no newlines
        if(cleaned.length() > 100) cleaned = cleaned.substring(0, 100);
        return cleaned;
    }

    public static boolean isValidInitialAmount(double amount){
        return amount >= 0.0 && amount <= 10_000_000.0; // example business rule
    }
}
