package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

public class LocalizationService {
    private Locale locale;
    private ResourceBundle bundle;
    private final String baseName; // e.g. "i18n.messages"

    public LocalizationService(String baseName, Locale initial) {
        this.baseName = baseName;
        // try to set locale (will try classpath then filesystem)
        setLocaleWithFallback(initial);
    }

    private void setLocaleWithFallback(Locale locale) {
        if (locale == null) locale = Locale.getDefault();
        this.locale = locale;

        // 1) try normal ResourceBundle (classpath)
        try {
            this.bundle = ResourceBundle.getBundle(baseName, this.locale);
            return;
        } catch (MissingResourceException ignored) {
            // try filesystem fallback below
        }

        // 2) try loading properties file from project filesystem (useful during dev)
        // baseName like "i18n.messages" -> path "src/resources/i18n/messages_{lang}.properties"
        String basePath = baseName.replace('.', File.separatorChar);
        String langSuffix = locale.getLanguage();
        String filename = basePath + (langSuffix == null || langSuffix.isEmpty() ? ".properties" : ("_" + langSuffix + ".properties"));

        // candidate locations (relative to project root)
        String[] candidateDirs = new String[] {
                "src/resources",          // your current layout
                "src/main/resources",     // maven layout
                "."                       // project root (if you keep resources at root)
        };

        for (String dir : candidateDirs) {
            File f = new File(dir, filename);
            if (f.exists() && f.isFile()) {
                try (InputStream is = new FileInputStream(f)) {
                    this.bundle = new PropertyResourceBundle(is);
                    System.err.println("[Localization] Loaded bundle from file: " + f.getAbsolutePath());
                    return;
                } catch (Exception e) {
                    System.err.println("[Localization] Failed loading bundle from file " + f.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        // 3) final fallback: empty bundle that returns the key in brackets
        this.bundle = new ResourceBundle() {
            @Override protected Object handleGetObject(String key) { return "[" + key + "]"; }
            @Override public Enumeration<String> getKeys() { return Collections.emptyEnumeration(); }
        };
        System.err.println("[Localization] Warning: no resource bundle found for baseName=" + baseName + ", locale=" + locale);
    }

    public synchronized void setLocale(Locale locale) {
        setLocaleWithFallback(locale);
    }

    public Locale getLocale() { return locale; }

    public String t(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            pattern = "[" + key + "]"; // واضح لو الترجمة ناقصة
        }
        if (args == null || args.length == 0) return pattern;
        return MessageFormat.format(pattern, args);
    }
}
