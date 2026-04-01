package com.ocr.nospring;

/**
 * Utility class for Tesseract language detection and configuration.
 * Provides static helper methods to determine OCR engine selection
 * and language mapping for various non-Latin scripts.
 */
public final class TesseractLanguageHelper {

    private TesseractLanguageHelper() {
        // Utility class, prevent instantiation
    }

    public static boolean isHebrew(String language) {
        return "he".equalsIgnoreCase(language) || "hebrew".equalsIgnoreCase(language);
    }

    public static boolean isThai(String language) {
        return "th".equalsIgnoreCase(language) || "tha".equalsIgnoreCase(language) || "thai".equalsIgnoreCase(language);
    }

    public static boolean isRussian(String language) {
        return "ru".equalsIgnoreCase(language) || "rus".equalsIgnoreCase(language) || "russian".equalsIgnoreCase(language);
    }

    public static boolean isPersian(String language) {
        return "fa".equalsIgnoreCase(language) || "fas".equalsIgnoreCase(language) || "persian".equalsIgnoreCase(language) || "farsi".equalsIgnoreCase(language);
    }

    public static boolean isArabic(String language) {
        return "ar".equalsIgnoreCase(language) || "ara".equalsIgnoreCase(language) || "arabic".equalsIgnoreCase(language);
    }

    public static boolean isUkrainian(String language) {
        return "uk".equalsIgnoreCase(language) || "ukr".equalsIgnoreCase(language) || "ukrainian".equalsIgnoreCase(language);
    }

    public static boolean isBulgarian(String language) {
        return "bg".equalsIgnoreCase(language) || "bul".equalsIgnoreCase(language) || "bulgarian".equalsIgnoreCase(language);
    }

    public static boolean isSerbian(String language) {
        return "sr".equalsIgnoreCase(language) || "srp".equalsIgnoreCase(language) || "serbian".equalsIgnoreCase(language);
    }

    public static boolean isMacedonian(String language) {
        return "mk".equalsIgnoreCase(language) || "mkd".equalsIgnoreCase(language) || "macedonian".equalsIgnoreCase(language);
    }

    public static boolean isBelarusian(String language) {
        return "be".equalsIgnoreCase(language) || "bel".equalsIgnoreCase(language) || "belarusian".equalsIgnoreCase(language);
    }

    public static boolean isGreek(String language) {
        return "el".equalsIgnoreCase(language) || "ell".equalsIgnoreCase(language) || "gre".equalsIgnoreCase(language) || "greek".equalsIgnoreCase(language) || "grc".equalsIgnoreCase(language);
    }

    public static boolean isHindi(String language) {
        return "hi".equalsIgnoreCase(language) || "hin".equalsIgnoreCase(language) || "hindi".equalsIgnoreCase(language);
    }

    public static boolean isGujarati(String language) {
        return "gu".equalsIgnoreCase(language) || "guj".equalsIgnoreCase(language) || "gujarati".equalsIgnoreCase(language);
    }

    public static boolean isBengali(String language) {
        return "bn".equalsIgnoreCase(language) || "ben".equalsIgnoreCase(language) || "bengali".equalsIgnoreCase(language);
    }

    public static boolean isTamil(String language) {
        return "ta".equalsIgnoreCase(language) || "tam".equalsIgnoreCase(language) || "tamil".equalsIgnoreCase(language);
    }

    public static boolean isTelugu(String language) {
        return "te".equalsIgnoreCase(language) || "tel".equalsIgnoreCase(language) || "telugu".equalsIgnoreCase(language);
    }

    public static boolean isMarathi(String language) {
        return "mr".equalsIgnoreCase(language) || "mar".equalsIgnoreCase(language) || "marathi".equalsIgnoreCase(language);
    }

    public static boolean isUrdu(String language) {
        return "ur".equalsIgnoreCase(language) || "urd".equalsIgnoreCase(language) || "urdu".equalsIgnoreCase(language);
    }

    public static boolean isPashto(String language) {
        return "ps".equalsIgnoreCase(language) || "pus".equalsIgnoreCase(language) || "pashto".equalsIgnoreCase(language);
    }

    public static boolean isAmharic(String language) {
        return "am".equalsIgnoreCase(language) || "amh".equalsIgnoreCase(language) || "amharic".equalsIgnoreCase(language);
    }

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

    public static boolean useTesseract(String language) {
        return isHebrew(language) || isThai(language) || isRussian(language) || isPersian(language) || isArabic(language) || isUkrainian(language) || isBulgarian(language) || isSerbian(language) || isMacedonian(language) || isBelarusian(language) || isGreek(language) || isHindi(language) || isGujarati(language) || isBengali(language) || isTamil(language) || isTelugu(language) || isMarathi(language) || isUrdu(language) || isPashto(language) || isAmharic(language);
    }

    public static boolean shouldUseTesseract(String engine, String language) {
        if ("tesseract".equals(engine)) return true;
        if ("rapidocr".equals(engine)) return false;
        // Explicit RapidOCR detection for chi_tra, chi_sim, eng (per architecture rule)
        if (isChineseOrEnglish(language)) return false;
        return useTesseract(language);
    }

    public static String getTesseractLanguage(String language) {
        if (isHebrew(language)) return "heb+eng";
        if (isThai(language)) return "tha+eng";
        if (isRussian(language)) return "rus+eng";
        if (isPersian(language)) return "ara+eng";
        if (isArabic(language)) return "ara+eng";
        if (isUkrainian(language)) return "ukr+eng";
        if (isBulgarian(language)) return "bul+eng";
        if (isSerbian(language)) return "srp+eng";
        if (isMacedonian(language)) return "mkd+eng";
        if (isBelarusian(language)) return "bel+eng";
        if (isGreek(language)) return "ell+eng";
        if (isHindi(language)) return "hin+eng";
        if (isGujarati(language)) return "guj+eng";
        if (isBengali(language)) return "ben+eng";
        if (isTamil(language)) return "tam+eng";
        if (isTelugu(language)) return "tel+eng";
        if (isMarathi(language)) return "mar+eng";
        if (isUrdu(language)) return "urd+eng";
        if (isPashto(language)) return "pus+eng";
        if (isAmharic(language)) return "amh+eng";
        return "eng";
    }

    public static String getTesseractLabel(String language) {
        if (isHebrew(language)) return "Hebrew";
        if (isThai(language)) return "Thai";
        if (isRussian(language)) return "Russian";
        if (isPersian(language)) return "Persian";
        if (isArabic(language)) return "Arabic";
        if (isUkrainian(language)) return "Ukrainian";
        if (isBulgarian(language)) return "Bulgarian";
        if (isSerbian(language)) return "Serbian";
        if (isMacedonian(language)) return "Macedonian";
        if (isBelarusian(language)) return "Belarusian";
        if (isGreek(language)) return "Greek";
        if (isHindi(language)) return "Hindi";
        if (isGujarati(language)) return "Gujarati";
        if (isBengali(language)) return "Bengali";
        if (isTamil(language)) return "Tamil";
        if (isTelugu(language)) return "Telugu";
        if (isMarathi(language)) return "Marathi";
        if (isUrdu(language)) return "Urdu";
        if (isPashto(language)) return "Pashto";
        if (isAmharic(language)) return "Amharic";
        return language;
    }
}
