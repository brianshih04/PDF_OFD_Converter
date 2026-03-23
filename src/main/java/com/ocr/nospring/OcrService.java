package com.ocr.nospring;

import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.Model;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 服務 - 無 Spring Boot
 */
public class OcrService {
    
    private InferenceEngine engine;
    private boolean initialized = false;
    
    public OcrService() {
        // 不在構造函數中初始化
    }
    
    public void initialize() throws Exception {
        if (initialized) return;
        
        System.out.println("  Initializing OCR engine...");
        engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        initialized = true;
        System.out.println("  OK: OCR engine initialized");
    }
    
    public List<TextBlock> recognize(BufferedImage image, String language) throws Exception {
        if (!initialized) {
            initialize();
        }
        
        // 保存圖片到臨時文件
        File tempFile = File.createTempFile("ocr_", ".png");
        ImageIO.write(image, "PNG", tempFile);
        
        // 執行 OCR
        com.benjaminwan.ocrlibrary.OcrResult rapidResult = engine.runOcr(tempFile.getAbsolutePath());
        
        List<TextBlock> textBlocks = new ArrayList<>();
        
        java.util.ArrayList<com.benjaminwan.ocrlibrary.TextBlock> blocks = rapidResult.getTextBlocks();
        
        if (blocks != null && !blocks.isEmpty()) {
            for (com.benjaminwan.ocrlibrary.TextBlock block : blocks) {
                String text = block.getText();
                if (text != null && !text.trim().isEmpty()) {
                    java.util.ArrayList<com.benjaminwan.ocrlibrary.Point> boxPoints = block.getBoxPoint();
                    
                    double minX = boxPoints.stream().mapToInt(com.benjaminwan.ocrlibrary.Point::getX).min().orElse(0);
                    double minY = boxPoints.stream().mapToInt(com.benjaminwan.ocrlibrary.Point::getY).min().orElse(0);
                    double maxX = boxPoints.stream().mapToInt(com.benjaminwan.ocrlibrary.Point::getX).max().orElse(0);
                    double maxY = boxPoints.stream().mapToInt(com.benjaminwan.ocrlibrary.Point::getY).max().orElse(0);
                    
                    TextBlock tb = new TextBlock();
                    tb.text = text;
                    tb.x = minX;
                    tb.y = minY;
                    tb.width = maxX - minX;
                    tb.height = maxY - minY;
                    tb.confidence = 0.9; // Default confidence
                    tb.fontSize = calculateFontSize(tb.height);
                    textBlocks.add(tb);
                }
            }
        }
        
        // 刪除臨時文件
        tempFile.delete();
        
        return textBlocks;
    }
    
    private float calculateFontSize(double height) {
        // 簡單的字體大小估算
        return (float) (height * 0.8);
    }
    
    /**
     * 文字塊
     */
    public static class TextBlock {
        public String text;
        public double x;
        public double y;
        public double width;
        public double height;
        public double confidence;
        public float fontSize;
    }
}
