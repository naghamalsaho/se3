package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public class LocalizationService {
    private Locale locale;
    private ResourceBundle bundle;
    private final String baseName; // e.g. "i18n.messages"

    public LocalizationService(String baseName, Locale initial) {
        this.baseName = baseName;
        setLocaleWithFallback(initial);
    }

    private void setLocaleWithFallback(Locale locale) {
        if (locale == null) locale = Locale.getDefault();
        this.locale = locale;

        // control that reads properties as UTF-8 from classpath
        ResourceBundle.Control utf8Control = new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                            ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                InputStream stream = loader.getResourceAsStream(resourceName);
                if (stream == null) return null;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    String content = sb.toString();
                    // strip BOM if present
                    if (content.startsWith("\uFEFF")) content = content.substring(1);
                    return new PropertyResourceBundle(new StringReader(content));
                }
            }
        };

        // 1) try loading from classpath using UTF-8 control
        try {
            this.bundle = ResourceBundle.getBundle(baseName, this.locale, utf8Control);
            System.err.println("[Localization] Loaded bundle from classpath: " + baseName + " locale=" + locale);
            return;
        } catch (MissingResourceException ignored) {
            // will try filesystem below
        }

        // 2) try filesystem fallback (useful during dev)
        String basePath = baseName.replace('.', File.separatorChar);
        String langSuffix = locale.getLanguage();
        String filename = basePath + (langSuffix == null || langSuffix.isEmpty() ? ".properties" : ("_" + langSuffix + ".properties"));

        String[] candidateDirs = new String[] {
                "src/resources",
                "src/main/resources",
                "."
        };

        for (String dir : candidateDirs) {
            File f = new File(dir, filename);
            if (f.exists() && f.isFile()) {
                try (InputStream is = new FileInputStream(f);
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    String content = sb.toString();
                    if (content.startsWith("\uFEFF")) content = content.substring(1); // remove BOM
                    this.bundle = new PropertyResourceBundle(new StringReader(content));
                    System.err.println("[Localization] Loaded bundle from file: " + f.getAbsolutePath());
                    return;
                } catch (Exception e) {
                    System.err.println("[Localization] Failed loading bundle from file " + f.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        // 3) fallback
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
            pattern = "[" + key + "]";
        }
        if (args == null || args.length == 0) return pattern;
        return MessageFormat.format(pattern, args);
    }
}
