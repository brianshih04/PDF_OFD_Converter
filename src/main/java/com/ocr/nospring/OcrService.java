package com.ocr.nospring;

import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 服務 - 無 Spring Boot
 */
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    /** default confidence value assigned to OCR text results */
    private static final double DEFAULT_CONFIDENCE = 0.9;

    private InferenceEngine engine;
    private volatile boolean initialized = false;
    
    public OcrService() {
        // 不在構造函數中初始化
    }
    
    public synchronized void initialize() throws Exception {
        if (initialized) return;

        log.info("  Initializing OCR engine...");
        engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        initialized = true;
        log.info("  OK: OCR engine initialized");
    }
    
    public List<TextBlock> recognize(BufferedImage image, String language) throws Exception {
        if (!initialized) {
            initialize();
        }

        File tempFile = null;
        try {
            // 保存圖片到臨時文件
            tempFile = File.createTempFile("ocr_", ".png");
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
                        tb.confidence = DEFAULT_CONFIDENCE;
                        tb.fontSize = calculateFontSize(tb.height);
                        textBlocks.add(tb);
                    }
                }
            }

            return textBlocks;
        } finally {
            // 刪除臨時文件
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
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
