package com.ocr.nospring;

/**
 * 配置類 - 無 Spring Boot
 */
public class Config {
    
    private String fontPath;
    
    public Config() {
        this.fontPath = getDefaultFontPath();
    }
    
    private String getDefaultFontPath() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            String[] windowsFonts = {
                "C:/Windows/Fonts/msjh.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/simsun.ttc"
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
    
    public void setFontPath(String fontPath) {
        if (fontPath != null && !fontPath.isEmpty()) {
            this.fontPath = fontPath;
        }
    }
}
