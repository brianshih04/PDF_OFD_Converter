package com.ocr.nospring;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for Tesseract language detection and configuration.
 * Provides static helper methods to determine OCR engine selection
 * and language mapping for various non-Latin scripts.
 *
 * Language data is driven by {@link #TESSERACT_LANGUAGES} so that adding a
 * new language requires only a single map entry instead of multiple methods.
 */
public final class TesseractLanguageHelper {

    private TesseractLanguageHelper() {
        // Utility class, prevent instantiation
    }

    /**
     * Data-driven registry of Tesseract-supported languages.
     * Each entry maps a canonical key (e.g. "hebrew") to its recognition data:
     * the set of acceptable input aliases, the Tesseract lang string, and a display label.
     */
    private static final Map<String, LanguageEntry> TESSERACT_LANGUAGES = Map.ofEntries(
        entry("hebrew",     Set.of("he", "heb"),            "heb+eng", "Hebrew"),
        entry("thai",       Set.of("th", "tha"),            "tha+eng", "Thai"),
        entry("russian",    Set.of("ru", "rus"),            "rus+eng", "Russian"),
        entry("persian",    Set.of("fa", "fas", "farsi"),   "ara+eng", "Persian"),
        entry("arabic",     Set.of("ar", "ara"),            "ara+eng", "Arabic"),
        entry("ukrainian",  Set.of("uk", "ukr"),            "ukr+eng", "Ukrainian"),
        entry("bulgarian",  Set.of("bg", "bul"),            "bul+eng", "Bulgarian"),
        entry("serbian",    Set.of("sr", "srp"),            "srp+eng", "Serbian"),
        entry("macedonian", Set.of("mk", "mkd"),            "mkd+eng", "Macedonian"),
        entry("belarusian", Set.of("be", "bel"),            "bel+eng", "Belarusian"),
        entry("greek",      Set.of("el", "ell", "gre", "grc"), "ell+eng", "Greek"),
        entry("hindi",      Set.of("hi", "hin"),            "hin+eng", "Hindi"),
        entry("gujarati",   Set.of("gu", "guj"),            "guj+eng", "Gujarati"),
        entry("bengali",    Set.of("bn", "ben"),            "ben+eng", "Bengali"),
        entry("tamil",      Set.of("ta", "tam"),            "tam+eng", "Tamil"),
        entry("telugu",     Set.of("te", "tel"),            "tel+eng", "Telugu"),
        entry("marathi",    Set.of("mr", "mar"),            "mar+eng", "Marathi"),
        entry("urdu",       Set.of("ur", "urd"),            "urd+eng", "Urdu"),
        entry("pashto",     Set.of("ps", "pus"),            "pus+eng", "Pashto"),
        entry("amharic",    Set.of("am", "amh"),            "amh+eng", "Amharic")
    );

    /** Flattened set of all accepted language aliases for O(1) lookup. */
    private static final Set<String> TESSERACT_ALIASES = buildAliasSet();

    /** Languages that should prefer RapidOCR over Tesseract. */
    private static final Set<String> RAPIDOCR_ALIASES = Set.of(
        "chi_tra", "chi_sim", "eng", "chinese_cht", "chinese", "english", "zh", "zh-tw", "zh-cn", "en"
    );

    private static Set<String> buildAliasSet() {
        Set<String> aliases = new java.util.HashSet<>();
        for (LanguageEntry entry : TESSERACT_LANGUAGES.values()) {
            aliases.addAll(entry.aliases());
            aliases.add(entry.key());
        }
        return Set.copyOf(aliases);
    }

    private static Map.Entry<String, LanguageEntry> entry(String key, Set<String> aliases,
                                                          String tesseractLang, String label) {
        return Map.entry(key, new LanguageEntry(key, aliases, tesseractLang, label));
    }

    /** Look up a {@link LanguageEntry} by any recognized alias or canonical key. */
    static LanguageEntry findEntry(String language) {
        if (language == null) return null;
        String lower = language.toLowerCase(Locale.ROOT);
        // Direct alias lookup
        if (TESSERACT_ALIASES.contains(lower)) {
            for (LanguageEntry entry : TESSERACT_LANGUAGES.values()) {
                if (entry.matches(lower)) return entry;
            }
        }
        return null;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns true if the given language is explicitly routed to RapidOCR
     * (chi_tra, chi_sim, eng, etc.).
     */
    public static boolean isChineseOrEnglish(String language) {
        if (language == null) return false;
        return RAPIDOCR_ALIASES.contains(language.toLowerCase(Locale.ROOT));
    }

    /** Returns true if the language is handled by Tesseract. */
    public static boolean useTesseract(String language) {
        return findEntry(language) != null;
    }

    public static boolean shouldUseTesseract(String engine, String language) {
        if ("tesseract".equals(engine)) return true;
        if ("rapidocr".equals(engine)) return false;
        if (isChineseOrEnglish(language)) return false;
        return useTesseract(language);
    }

    /**
     * Returns the Tesseract language string (e.g. "rus+eng") for the given language,
     * or "eng" as fallback.
     */
    public static String getTesseractLanguage(String language) {
        LanguageEntry entry = findEntry(language);
        return entry != null ? entry.tesseractLang() : "eng";
    }

    /**
     * Returns a human-readable label (e.g. "Russian") for the given language,
     * or the raw language string as fallback.
     */
    public static String getTesseractLabel(String language) {
        LanguageEntry entry = findEntry(language);
        return entry != null ? entry.label() : language;
    }

    // ── Inner record ────────────────────────────────────────────────────────

    record LanguageEntry(String key, Set<String> aliases, String tesseractLang, String label) {
        boolean matches(String input) {
            return key.equals(input) || aliases.contains(input);
        }
    }
}
