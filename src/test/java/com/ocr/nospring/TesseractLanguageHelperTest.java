package com.ocr.nospring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TesseractLanguageHelperTest {

    @Test
    void shouldUseTesseract_hebrew_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "hebrew"));
    }

    @Test
    void shouldUseTesseract_he_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "he"));
    }

    @Test
    void shouldUseTesseract_thai_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "thai"));
    }

    @Test
    void shouldUseTesseract_russian_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "russian"));
    }

    @Test
    void shouldUseTesseract_arabic_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "arabic"));
    }

    @Test
    void shouldUseTesseract_hindi_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("auto", "hindi"));
    }

    @Test
    void shouldUseTesseract_chineseCht_returnsFalse() {
        assertFalse(TesseractLanguageHelper.shouldUseTesseract("auto", "chinese_cht"));
    }

    @Test
    void shouldUseTesseract_english_returnsFalse() {
        assertFalse(TesseractLanguageHelper.shouldUseTesseract("auto", "en"));
    }

    @Test
    void shouldUseTesseract_forcedTesseract_returnsTrue() {
        assertTrue(TesseractLanguageHelper.shouldUseTesseract("tesseract", "en"));
    }

    @Test
    void shouldUseTesseract_forcedRapidocr_returnsFalse() {
        assertFalse(TesseractLanguageHelper.shouldUseTesseract("rapidocr", "hebrew"));
    }

    @Test
    void getTesseractLanguage_hebrew_returnsHebEng() {
        assertEquals("heb+eng", TesseractLanguageHelper.getTesseractLanguage("hebrew"));
    }

    @Test
    void getTesseractLanguage_russian_returnsRusEng() {
        assertEquals("rus+eng", TesseractLanguageHelper.getTesseractLanguage("russian"));
    }

    @Test
    void getTesseractLanguage_unknown_returnsEng() {
        assertEquals("eng", TesseractLanguageHelper.getTesseractLanguage("unknown_lang"));
    }

    @Test
    void getTesseractLabel_hebrew_returnsHebrew() {
        assertEquals("Hebrew", TesseractLanguageHelper.getTesseractLabel("hebrew"));
    }

    @Test
    void getTesseractLabel_he_alias_returnsHebrew() {
        assertEquals("Hebrew", TesseractLanguageHelper.getTesseractLabel("he"));
    }

    @Test
    void getTesseractLabel_unknown_returnsRaw() {
        assertEquals("xyz", TesseractLanguageHelper.getTesseractLabel("xyz"));
    }

    @Test
    void isChineseOrEnglish_chineseCht_returnsTrue() {
        assertTrue(TesseractLanguageHelper.isChineseOrEnglish("chinese_cht"));
    }

    @Test
    void isChineseOrEnglish_en_returnsTrue() {
        assertTrue(TesseractLanguageHelper.isChineseOrEnglish("en"));
    }

    @Test
    void isChineseOrEnglish_russian_returnsFalse() {
        assertFalse(TesseractLanguageHelper.isChineseOrEnglish("russian"));
    }
}
