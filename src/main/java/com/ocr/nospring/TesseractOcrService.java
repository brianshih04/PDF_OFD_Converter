package com.ocr.nospring;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.ITessAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * Tesseract OCR 服務 - 用於 RapidOCR 不支援的語系
 */
public class TesseractOcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    private final Tesseract tesseract;

    public TesseractOcrService(String dataPath, String language) throws Exception {
        tesseract = new Tesseract();
        if (dataPath != null && !dataPath.isEmpty()) {
            tesseract.setDatapath(dataPath);
        } else {
            // Default: look for tessdata next to the JAR/app
            String appDir = System.getProperty("user.dir");
            String defaultPath = appDir + "/tessdata";
            java.io.File f = new java.io.File(defaultPath);
            if (f.exists()) {
                tesseract.setDatapath(defaultPath);
            }
        }
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
    }

    public List<TextBlock> recognize(BufferedImage image) {
        List<TextBlock> textBlocks = new ArrayList<>();

        try {
            List<net.sourceforge.tess4j.Word> lines = tesseract.getWords(image,
                ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);

            if (lines == null || lines.isEmpty()) {
                return textBlocks;
            }

            for (net.sourceforge.tess4j.Word line : lines) {
                String text = line.getText();
                if (text != null && !text.trim().isEmpty()) {
                    java.awt.Rectangle rect = line.getBoundingBox();

                    TextBlock tb = new TextBlock();
                    tb.setText(text.trim());
                    tb.setX(rect.getX());
                    tb.setY(rect.getY());
                    tb.setWidth(rect.getWidth());
                    tb.setHeight(rect.getHeight());
                    tb.setConfidence(line.getConfidence() / 100.0);
                    tb.setFontSize((float) tb.getHeight());
                    textBlocks.add(tb);
                }
            }
        } catch (Exception e) {
            log.error("    Error in Tesseract recognize: {}", e.getMessage());
        }

        return textBlocks;
    }
}
