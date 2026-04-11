package com.ocr.nospring;

import java.util.Map;

/**
 * Utility class for Tesseract language detection and configuration.
 * Provides static helper methods to determine OCR engine selection
 * and language mapping for various non-Latin scripts.
 *
 * Uses a Map-driven architecture instead of individual boolean methods,
 * making it easy to add new language support with a single entry.
 */
public final class TesseractLanguageHelper {

    private TesseractLanguageHelper() {
        // Utility class, prevent instantiation
    }

    /**
     * Language mapping record: maps language keys to Tesseract language codes and display names.
     */
    private record LanguageMapping(String tessLang, String displayName) {}

    /**
     * All languages that should use Tesseract (RapidOCR doesn't support them well).
     * Each entry maps multiple possible language keys (2-letter ISO, 3-letter ISO, full name)
     * to a single Tesseract language code and display name.
     */
    private static final Map<String, LanguageMapping> TESSERACT_LANGUAGES = Map.ofEntries(
        Map.entry("he", new LanguageMapping("heb+eng", "Hebrew")),
        Map.entry("hebrew", new LanguageMapping("heb+eng", "Hebrew")),
        Map.entry("th", new LanguageMapping("tha+eng", "Thai")),
        Map.entry("tha", new LanguageMapping("tha+eng", "Thai")),
        Map.entry("thai", new LanguageMapping("tha+eng", "Thai")),
        Map.entry("ru", new LanguageMapping("rus+eng", "Russian")),
        Map.entry("rus", new LanguageMapping("rus+eng", "Russian")),
        Map.entry("russian", new LanguageMapping("rus+eng", "Russian")),
        Map.entry("fa", new LanguageMapping("fas+eng", "Persian")),
        Map.entry("fas", new LanguageMapping("fas+eng", "Persian")),
        Map.entry("farsi", new LanguageMapping("fas+eng", "Persian")),
        Map.entry("persian", new LanguageMapping("fas+eng", "Persian")),
        Map.entry("ar", new LanguageMapping("ara+eng", "Arabic")),
        Map.entry("ara", new LanguageMapping("ara+eng", "Arabic")),
        Map.entry("arabic", new LanguageMapping("ara+eng", "Arabic")),
        Map.entry("uk", new LanguageMapping("ukr+eng", "Ukrainian")),
        Map.entry("ukr", new LanguageMapping("ukr+eng", "Ukrainian")),
        Map.entry("ukrainian", new LanguageMapping("ukr+eng", "Ukrainian")),
        Map.entry("bg", new LanguageMapping("bul+eng", "Bulgarian")),
        Map.entry("bul", new LanguageMapping("bul+eng", "Bulgarian")),
        Map.entry("bulgarian", new LanguageMapping("bul+eng", "Bulgarian")),
        Map.entry("sr", new LanguageMapping("srp+eng", "Serbian")),
        Map.entry("srp", new LanguageMapping("srp+eng", "Serbian")),
        Map.entry("serbian", new LanguageMapping("srp+eng", "Serbian")),
        Map.entry("mk", new LanguageMapping("mkd+eng", "Macedonian")),
        Map.entry("mkd", new LanguageMapping("mkd+eng", "Macedonian")),
        Map.entry("macedonian", new LanguageMapping("mkd+eng", "Macedonian")),
        Map.entry("be", new LanguageMapping("bel+eng", "Belarusian")),
        Map.entry("bel", new LanguageMapping("bel+eng", "Belarusian")),
        Map.entry("belarusian", new LanguageMapping("bel+eng", "Belarusian")),
        Map.entry("el", new LanguageMapping("ell+eng", "Greek")),
        Map.entry("ell", new LanguageMapping("ell+eng", "Greek")),
        Map.entry("gre", new LanguageMapping("ell+eng", "Greek")),
        Map.entry("greek", new LanguageMapping("ell+eng", "Greek")),
        Map.entry("grc", new LanguageMapping("ell+eng", "Greek")),
        Map.entry("hi", new LanguageMapping("hin+eng", "Hindi")),
        Map.entry("hin", new LanguageMapping("hin+eng", "Hindi")),
        Map.entry("hindi", new LanguageMapping("hin+eng", "Hindi")),
        Map.entry("gu", new LanguageMapping("guj+eng", "Gujarati")),
        Map.entry("guj", new LanguageMapping("guj+eng", "Gujarati")),
        Map.entry("gujarati", new LanguageMapping("guj+eng", "Gujarati")),
        Map.entry("bn", new LanguageMapping("ben+eng", "Bengali")),
        Map.entry("ben", new LanguageMapping("ben+eng", "Bengali")),
        Map.entry("bengali", new LanguageMapping("ben+eng", "Bengali")),
        Map.entry("ta", new LanguageMapping("tam+eng", "Tamil")),
        Map.entry("tam", new LanguageMapping("tam+eng", "Tamil")),
        Map.entry("tamil", new LanguageMapping("tam+eng", "Tamil")),
        Map.entry("te", new LanguageMapping("tel+eng", "Telugu")),
        Map.entry("tel", new LanguageMapping("tel+eng", "Telugu")),
        Map.entry("telugu", new LanguageMapping("tel+eng", "Telugu")),
        Map.entry("mr", new LanguageMapping("mar+eng", "Marathi")),
        Map.entry("mar", new LanguageMapping("mar+eng", "Marathi")),
        Map.entry("marathi", new LanguageMapping("mar+eng", "Marathi")),
        Map.entry("ur", new LanguageMapping("urd+eng", "Urdu")),
        Map.entry("urd", new LanguageMapping("urd+eng", "Urdu")),
        Map.entry("urdu", new LanguageMapping("urd+eng", "Urdu")),
        Map.entry("ps", new LanguageMapping("pus+eng", "Pashto")),
        Map.entry("pus", new LanguageMapping("pus+eng", "Pashto")),
        Map.entry("pashto", new LanguageMapping("pus+eng", "Pashto")),
        Map.entry("am", new LanguageMapping("amh+eng", "Amharic")),
        Map.entry("amh", new LanguageMapping("amh+eng", "Amharic")),
        Map.entry("amharic", new LanguageMapping("amh+eng", "Amharic"))
    );

    /**
     * Explicit detection for chi_tra, chi_sim, eng to ensure RapidOCR is used.
     * Per architecture rule: chi_tra, chi_sim, eng 優先使用 RapidOCR.
     */
    public static boolean isChineseOrEnglish(String language) {
        if (language == null) return false;
        String lower = language.toLowerCase();
        return lower.equals("chi_tra") || lower.equals("chi_sim") || lower.equals("eng")
            || lower.equals("chinese_cht") || lower.equals("chinese") || lower.equals("english")
            || lower.equals("zh") || lower.equals("zh-tw") || lower.equals("zh-cn")
            || lower.equals("en");
    }

    /**
     * Resolve a language key to its normalized form for map lookup.
     */
    private static String normalizeLanguageKey(String language) {
        if (language == null) return "";
        String key = language.toLowerCase().trim();
        // Direct lookup first
        if (TESSERACT_LANGUAGES.containsKey(key)) return key;
        // Try prefix (e.g., "chinese_cht" -> "chinese" won't match, but "fa_anything" -> "fa")
        int underscoreIdx = key.indexOf('_');
        if (underscoreIdx > 0) {
            String prefix = key.substring(0, underscoreIdx);
            if (TESSERACT_LANGUAGES.containsKey(prefix)) return prefix;
        }
        return key;
    }

    /**
     * Look up the LanguageMapping for a given language key.
     * Returns null if the language is not in the Tesseract-supported map.
     */
    private static LanguageMapping lookupLanguage(String language) {
        return TESSERACT_LANGUAGES.get(normalizeLanguageKey(language));
    }

    /**
     * Determine if Tesseract should be used for a given engine and language.
     */
    public static boolean shouldUseTesseract(String engine, String language) {
        if ("tesseract".equals(engine)) return true;
        if ("rapidocr".equals(engine)) return false;
        // Explicit RapidOCR detection for chi_tra, chi_sim, eng (per architecture rule)
        if (isChineseOrEnglish(language)) return false;
        // Auto mode: check if language is in Tesseract map
        return lookupLanguage(language) != null;
    }

    /**
     * Get the Tesseract language code for a given language key.
     * Returns "eng" as default if not found.
     */
    public static String getTesseractLanguage(String language) {
        LanguageMapping mapping = lookupLanguage(language);
        return mapping != null ? mapping.tessLang() : "eng";
    }

    /**
     * Get the human-readable display name for a given language key.
     * Returns the raw language string if not found in the map.
     */
    public static String getTesseractLabel(String language) {
        LanguageMapping mapping = lookupLanguage(language);
        return mapping != null ? mapping.displayName() : language;
    }
}
