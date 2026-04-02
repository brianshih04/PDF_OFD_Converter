package com.ocr.nospring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置類 - 無 Spring Boot
 */
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    /** default text layer opacity — nearly invisible for searchable output */
    private static final double DEFAULT_TEXT_LAYER_OPACITY = 0.0001;

    // 簡繁轉換：null(不轉換), "s2t"(簡→繁), "t2s"(繁→簡)
    private String textConvert;

    private String fontPath;

    // OCR 語言（從 config 讀取）
    private String ocrLanguage;

    // Tesseract tessdata 路徑
    private String tesseractDataPath;

    // 文字層顏色 (RGB)
    private int textLayerRed = 255;
    private int textLayerGreen = 255;
    private int textLayerBlue = 255;

    // 文字層透明度 (0.0 - 1.0)
    private double textLayerOpacity = DEFAULT_TEXT_LAYER_OPACITY;

    public Config() {
        this.fontPath = getDefaultFontPath();
    }

    private String getDefaultFontPath() {
        // Primary: GoNotoKurrent-Regular.ttf (per architecture rule)
        String[] primaryFonts = {
            "fonts/GoNotoKurrent-Regular.ttf",
        };
        for (String font : primaryFonts) {
            java.io.File fontFile = new java.io.File(font);
            if (fontFile.exists()) {
                return font;
            }
        }

        // Fallback: wqy-ZenHei.ttf (per architecture rule)
        String[] fallbackFonts = {
            "fonts/wqy-ZenHei.ttf",
        };
        for (String font : fallbackFonts) {
            java.io.File fontFile = new java.io.File(font);
            if (fontFile.exists()) {
                return font;
            }
        }

        return null;
    }

    public String getFontPath() {
        return fontPath;
    }

    public String getOcrLanguage() { return ocrLanguage; }

    public void setOcrLanguage(String ocrLanguage) { this.ocrLanguage = ocrLanguage; }

    public String getTesseractDataPath() { return tesseractDataPath; }

    public void setTesseractDataPath(String path) { this.tesseractDataPath = path; }

    public void setFontPath(String fontPath) {
        if (fontPath != null && !fontPath.isEmpty()) {
            this.fontPath = fontPath;
        }
    }

    public String getTextConvert() {
        return textConvert;
    }

    public void setTextConvert(String textConvert) {
        this.textConvert = textConvert;
    }

    public int getTextLayerRed() {
        return textLayerRed;
    }

    public void setTextLayerRed(int textLayerRed) {
        this.textLayerRed = textLayerRed;
    }

    public int getTextLayerGreen() {
        return textLayerGreen;
    }

    public void setTextLayerGreen(int textLayerGreen) {
        this.textLayerGreen = textLayerGreen;
    }

    public int getTextLayerBlue() {
        return textLayerBlue;
    }

    public void setTextLayerBlue(int textLayerBlue) {
        this.textLayerBlue = textLayerBlue;
    }

    public double getTextLayerOpacity() {
        return textLayerOpacity;
    }

    public void setTextLayerOpacity(double textLayerOpacity) {
        this.textLayerOpacity = textLayerOpacity;
    }

    /**
     * 設定文字層顏色（支持顏色名稱或 RGB）
     * @param colorName 顏色名稱：white, red, black, blue, green, debug
     */
    public void setTextLayerColor(String colorName) {
        if (colorName == null) return;

        switch (colorName.toLowerCase()) {
            case "white":
                textLayerRed = 255; textLayerGreen = 255; textLayerBlue = 255;
                break;
            case "red":
                textLayerRed = 255; textLayerGreen = 0; textLayerBlue = 0;
                break;
            case "black":
                textLayerRed = 0; textLayerGreen = 0; textLayerBlue = 0;
                break;
            case "blue":
                textLayerRed = 0; textLayerGreen = 0; textLayerBlue = 255;
                break;
            case "green":
                textLayerRed = 0; textLayerGreen = 255; textLayerBlue = 0;
                break;
            case "debug":
                // 調試模式：紅色 + 不透明
                textLayerRed = 255; textLayerGreen = 0; textLayerBlue = 0;
                textLayerOpacity = 1.0;
                break;
        }
    }

    /**
     * Validate configuration parameters
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        // Validate font path if set
        if (fontPath != null && !fontPath.isEmpty()) {
            java.io.File fontFile = new java.io.File(fontPath);
            if (!fontFile.exists()) {
                throw new IllegalArgumentException("Font file not found: " + fontPath);
            }
            // Check if it's a TTC file (not supported by PDFBox)
            if (fontPath.toLowerCase().endsWith(".ttc")) {
                log.warn("WARNING: Font path points to a TTC file ({})", fontPath);
                log.warn("         TTC files are not supported by PDFBox. Please use a TTF font.");
            }
        } else {
            log.warn("WARNING: No TTF font found. Using default system font (English only).");
            log.warn("         For CJK language support, please specify a TTF font in config.");
        }

        // Validate opacity range
        if (textLayerOpacity < 0.0 || textLayerOpacity > 1.0) {
            throw new IllegalArgumentException("Text layer opacity must be between 0.0 and 1.0, got: " + textLayerOpacity);
        }

        // Validate textConvert values
        if (textConvert != null && !textConvert.isEmpty() &&
            !textConvert.equalsIgnoreCase("s2t") && !textConvert.equalsIgnoreCase("t2s")) {
            throw new IllegalArgumentException("Invalid textConvert value: " + textConvert +
                ". Valid values are: s2t, t2s, or null");
        }
    }
}
