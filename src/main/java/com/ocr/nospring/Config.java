package com.ocr.nospring;

/**
 * 配置類 - 無 Spring Boot
 */
public class Config {
    
    // 簡繁轉換：null(不轉換), "s2t"(簡→繁), "t2s"(繁→簡)
    private String textConvert;
    
    private String fontPath;
    
    // 文字層顏色 (RGB)
    private int textLayerRed = 255;
    private int textLayerGreen = 255;
    private int textLayerBlue = 255;
    
    // 文字層透明度 (0.0 - 1.0)
    private double textLayerOpacity = 0.0001;
    
    public Config() {
        this.fontPath = getDefaultFontPath();
    }
    
    private String getDefaultFontPath() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            String[] windowsFonts = {
                "C:/Windows/Fonts/simhei.ttf",      // 黑體 (TTF, 優先)
                "C:/Windows/Fonts/arial.ttf",       // Arial (TTF)
                "C:/Windows/Fonts/ariblk.ttf",      // Arial Black (TTF)
                "C:/Windows/Fonts/arialuni.ttf",    // Arial Unicode MS (TTF)
                "C:/Windows/Fonts/times.ttf",       // Times New Roman (TTF)
                "C:/Windows/Fonts/calibri.ttf",     // Calibri (TTF)
                "C:/Windows/Fonts/meiryo.ttc",      // Meiryo (日文字體，支援中文)
                "C:/Windows/Fonts/msyh.ttc",        // 微軟雅黑 (TTC)
                "C:/Windows/Fonts/msjh.ttc",        // 正黑體 (TTC)
                "C:/Windows/Fonts/simsun.ttc"       // 宋體 (TTC)
            };
            
            for (String font : windowsFonts) {
                if (new java.io.File(font).exists()) {
                    return font;
                }
            }
        }
        
        return null;
    }
    
    public String getFontPath() {
        return fontPath;
    }
    
    public String getOcrLanguage() { return null; }

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
}
