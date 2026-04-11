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
 * OCR 服務 - 使用 RapidOCR (ONNX PaddleOCR v4)
 */
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

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

        // RapidOCR API only accepts file path (String), not InputStream.
        // TODO: If a future version supports byte[]/InputStream, switch to in-memory stream.
        File tempFile = null;
        try {
            tempFile = File.createTempFile("ocr_", ".png");
            ImageIO.write(image, "PNG", tempFile);
            log.debug("  Temp file for OCR: {} ({} bytes)", tempFile.getAbsolutePath(), tempFile.length());

            com.benjaminwan.ocrlibrary.OcrResult rapidResult =
                engine.runOcr(tempFile.getAbsolutePath());

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
                        tb.setText(text);
                        tb.setX(minX);
                        tb.setY(minY);
                        tb.setWidth(maxX - minX);
                        tb.setHeight(maxY - minY);
                        // TODO: RapidOCR TextBlock does not expose per-block confidence; using placeholder
                        tb.setConfidence(0.9);
                        tb.setFontSize(calculateFontSize(tb.getHeight()));
                        textBlocks.add(tb);
                    }
                }
            }

            return textBlocks;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private float calculateFontSize(double height) {
        return (float) (height * 0.8);
    }
}
