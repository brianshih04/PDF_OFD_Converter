package com.ocr.nospring;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 服務 - 無 Spring Boot（使用與 OFD 相同的逐字符定位算法）
 */
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final Config config;

    public PdfService(Config config) {
        this.config = config;
    }

    public void generatePdf(BufferedImage image, List<TextBlock> textBlocks, File outputFile) throws Exception {

        try (PDDocument document = new PDDocument()) {
            // 載入字體
            PDFont font = loadFont(document);

            // 建立頁面
            float width = image.getWidth();
            float height = image.getHeight();
            PDPage page = new PDPage(new PDRectangle(width, height));
            document.addPage(page);

            // 轉換圖片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                document, imageBytes, "image"
            );

            // 繪製內容
            try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
            )) {
                // 1. 繪製圖片
                contentStream.drawImage(pdImage, 0, 0, width, height);

                // 2. 繪製透明文字層（使用與 OFD 相同的算法）
                drawTransparentTextLayer(contentStream, textBlocks, font, width, height);
            }

            // 保存
            document.save(outputFile);
        }
    }

    /**
     * 生成多頁 PDF
     */
    public void generateMultiPagePdf(List<BufferedImage> images, List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {

        if (images.size() != allTextBlocks.size()) {
            throw new IllegalArgumentException("Images and text blocks count mismatch");
        }

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadFont(document);

            for (int pageIndex = 0; pageIndex < images.size(); pageIndex++) {
                BufferedImage image = images.get(pageIndex);
                List<TextBlock> textBlocks = allTextBlocks.get(pageIndex);

                float width = image.getWidth();
                float height = image.getHeight();
                PDPage page = new PDPage(new PDRectangle(width, height));
                document.addPage(page);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                byte[] imageBytes = baos.toByteArray();

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    document, imageBytes, "image"
                );

                try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                    drawTransparentTextLayer(contentStream, textBlocks, font, width, height);
                }
            }

            document.save(outputFile);
        }
    }

    public void generateMultiPagePdfFromFiles(List<File> imageFiles, List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {

        if (imageFiles.size() != allTextBlocks.size()) {
            throw new IllegalArgumentException("Files and text blocks count mismatch");
        }

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadFont(document);

            for (int pageIndex = 0; pageIndex < imageFiles.size(); pageIndex++) {
                File imageFile = imageFiles.get(pageIndex);
                List<TextBlock> textBlocks = allTextBlocks.get(pageIndex);

                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    log.warn("  Skipping page {}: cannot read {}", pageIndex + 1, imageFile.getName());
                    continue;
                }

                float width = image.getWidth();
                float height = image.getHeight();
                PDPage page = new PDPage(new PDRectangle(width, height));
                document.addPage(page);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                byte[] imageBytes = baos.toByteArray();
                image.flush();

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    document, imageBytes, "image"
                );

                try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                    drawTransparentTextLayer(contentStream, textBlocks, font, width, height);
                }
            }

            document.save(outputFile);
        }
    }

    /**
     * 繪製透明文字層（整段定位，不用逐字/scaleX）
     */
    private void drawTransparentTextLayer(PDPageContentStream contentStream, List<TextBlock> textBlocks, PDFont font, float width, float height) throws Exception {
        // 設置透明度
        org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState extGState = new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
        float opacity = (float) config.getTextLayerOpacity();
        extGState.setNonStrokingAlphaConstant(opacity);
        extGState.setStrokingAlphaConstant(opacity);
        contentStream.setGraphicsStateParameters(extGState);

        // 設置顏色
        contentStream.setRenderingMode(RenderingMode.FILL);
        contentStream.setNonStrokingColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());

        contentStream.beginText();

        for (TextBlock block : textBlocks) {
            try {
                String text = block.getText().trim();
                if (text == null || text.isEmpty()) continue;

                double ocrX = block.getX();
                double ocrY = block.getY();
                double ocrW = block.getWidth();
                double ocrH = block.getHeight();

                // fontSize = box 高度
                float fontSizePt = (float) ocrH;

                // 計算文字自然寬度
                float textWidth = font.getStringWidth(text) / 1000f * fontSizePt;

                // 如果太寬就縮小 fontSize
                if (textWidth > ocrW) {
                    fontSizePt = (float) (fontSizePt * (ocrW / textWidth));
                    textWidth = font.getStringWidth(text) / 1000f * fontSizePt;
                }

                // Y: 文字底部往上抬一點（約 0.1 * fontSize）
                float pdfY = (float) (height - ocrY - ocrH + ocrH * 0.1);

                // 判斷直列文字
                boolean isVertical = ocrH > ocrW * 1.5;

                if (isVertical) {
                    // 直列：逐字從上到下
                    for (int i = 0; i < text.length(); i++) {
                        String ch = String.valueOf(text.charAt(i));
                        try {
                            float chW = font.getStringWidth(ch) / 1000f * fontSizePt;
                            float chX = (float) (ocrX + (ocrW - chW) / 2);
                            float chY = (float) (height - ocrY - fontSizePt * (i + 1));
                            contentStream.setFont(font, fontSizePt);
                            contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, chX, chY));
                            contentStream.showText(ch);
                        } catch (Exception e) {
                            log.warn("    [WARN] Skip char '{}': {}", ch, e.getMessage());
                        }
                    }
                } else {
                    // 橫列：整段定位，左對齊
                    contentStream.setFont(font, fontSizePt);
                    contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, (float) ocrX, pdfY));
                    contentStream.showText(text);
                }
            } catch (Exception e) {
                log.error("    Error drawing text: {}", e.getMessage());
            }
        }

        contentStream.endText();
    }

    /**
     * 載入字體 (per architecture rule: GoNotoKurrent primary, wqy-ZenHei fallback)
     */
    private PDFont loadFont(PDDocument document) throws Exception {
        String fontPath = config.getFontPath();

        // 1. 嘗試配置的字體（RTL 語言時跳過預設字型，改用 RTL 專用字型）
        String ocrLang = config.getOcrLanguage();
        boolean isRTL = ocrLang != null && (ocrLang.equals("he") || ocrLang.startsWith("ar") || ocrLang.equals("fa") || ocrLang.equals("ur"));

        if (fontPath != null && new File(fontPath).exists() && !isRTL) {
            try {
                PDFont font = PDType0Font.load(document, new File(fontPath));
                log.info("    Loaded font (config): {}", fontPath);
                return font;
            } catch (Exception e) {
                log.warn("    Warning: Cannot load font from {}: {}", fontPath, e.getMessage());
            }
        }

        // 2. RTL 語言（希伯來文、阿拉伯文等）— 跨平台字體偵測
        if (isRTL) {
            String[] rtlFontNames = {"tahoma.ttf", "segoeui.ttf", "DejaVuSans.ttf", "Arial.ttf"};
            for (String fontDir : getSystemFontDirectories()) {
                for (String fontName : rtlFontNames) {
                    File fontFile = new File(fontDir, fontName);
                    if (fontFile.exists()) {
                        try {
                            PDFont font = PDType0Font.load(document, fontFile);
                            log.info("    Loaded font (RTL): {}", fontFile.getAbsolutePath());
                            return font;
                        } catch (Exception e) {
                            // skip unsupported font format
                        }
                    }
                }
            }
        }

        // 3. Primary: GoNotoKurrent-Regular.ttf (per architecture rule)
        String[] primaryFonts = {
            "fonts/GoNotoKurrent-Regular.ttf",
        };
        for (String path : primaryFonts) {
            File fontFile = new File(path);
            if (fontFile.exists()) {
                try {
                    PDFont font = PDType0Font.load(document, fontFile);
                    log.info("    Loaded font (GoNotoKurrent): {}", path);
                    return font;
                } catch (Exception e) {
                }
            }
        }

        // 4. Fallback: wqy-ZenHei.ttf (per architecture rule)
        String[] fallbackFonts = {
            "fonts/wqy-ZenHei.ttf",
        };
        for (String path : fallbackFonts) {
            File fontFile = new File(path);
            if (fontFile.exists()) {
                try {
                    PDFont font = PDType0Font.load(document, fontFile);
                    log.info("    Loaded font (wqy-ZenHei fallback): {}", path);
                    return font;
                } catch (Exception e) {
                }
            }
        }

        // 5. 最後使用默認字體（僅支持英文）
        log.warn("    Warning: Using default Helvetica font (English only)");
        return org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
    }

    /**
     * 取得系統字體目錄（跨平台）
     */
    private String[] getSystemFontDirectories() {
        List<String> dirs = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String winDir = System.getenv("WINDIR");
            if (winDir != null) dirs.add(winDir + "\\Fonts");
        } else if (os.contains("mac")) {
            dirs.add("/System/Library/Fonts");
            dirs.add("/System/Library/Fonts/Supplemental");
            dirs.add("/Library/Fonts");
        } else {
            dirs.add("/usr/share/fonts/truetype");
            dirs.add("/usr/share/fonts/TTF");
            dirs.add("/usr/local/share/fonts");
        }
        return dirs.toArray(new String[0]);
    }
}
