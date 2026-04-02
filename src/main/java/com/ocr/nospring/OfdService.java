package com.ocr.nospring;

import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Img;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;
import org.ofdrw.layout.element.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD 服務 - 無 Spring Boot
 */
public class OfdService {

    private static final Logger log = LoggerFactory.getLogger(OfdService.class);

    /** mm per inch — unit conversion constant for pixel-to-mm conversion */
    private static final double MM_PER_INCH = 25.4;

    /** assumed screen DPI for pixel-to-mm coordinate conversion */
    private static final double ASSUMED_DPI = 72.0;

    /** font size as fraction of OCR box height (75% of box height) */
    private static final double FONT_SIZE_SCALE = 0.75;

    /** Y baseline position ratio within the OCR bounding box */
    private static final double TEXT_Y_BASELINE_RATIO = 0.72;

    /** fine-tuning offset ratio for Y axis positioning (moves text upward) */
    private static final double TEXT_Y_OFFSET_RATIO = 0.1;

    /** space character width as fraction of font size (fallback when AWT reports 0 width) */
    private static final double SPACE_CHAR_WIDTH_RATIO = 0.3;

    /** minimum paragraph width in mm to prevent unwanted line wrapping */
    private static final double PARAGRAPH_MIN_WIDTH_MM = 10.0;

    private final Config config;

    public OfdService(Config config) {
        this.config = config;
    }

    /**
     * 生成多頁 OFD (batch API — kept for backward compatibility)
     */
    public void generateMultiPageOfd(List<BufferedImage> images, List<List<OcrService.TextBlock>> allTextBlocks, File outputFile) throws Exception {
        openMultiPage(outputFile);
        try {
            for (int i = 0; i < images.size(); i++) {
                addPage(images.get(i), allTextBlocks.get(i), i + 1);
            }
        } finally {
            closeMultiPage();
        }
    }

    /**
     * Open a new multi-page OFD document. Caller must call addPage() then closeMultiPage().
     */
    public void openMultiPage(File outputFile) throws Exception {
        this.multiPageTempDir = Files.createTempDirectory("ofd_multipage_");
        this.multiPageTempImages = new ArrayList<>();
        this.multiPageOutputFile = outputFile;
        this.ofdDoc = new OFDDoc(outputFile.toPath());
    }

    /**
     * Add a single page (image + OCR text layer) to an open multi-page OFD.
     */
    public void addPage(BufferedImage image, List<OcrService.TextBlock> textBlocks, int pageNumber) throws Exception {
        Path tempImage = multiPageTempDir.resolve("page_" + (pageNumber - 1) + ".png");
        ImageIO.write(image, "PNG", tempImage.toFile());
        multiPageTempImages.add(tempImage);

        double widthMm = image.getWidth() * MM_PER_INCH / ASSUMED_DPI;
        double heightMm = image.getHeight() * MM_PER_INCH / ASSUMED_DPI;

        PageLayout pageLayout = new PageLayout(widthMm, heightMm);
        pageLayout.setMargin(0d);

        VirtualPage vPage = new VirtualPage(pageLayout);

        Img img = new Img(tempImage);
        img.setPosition(Position.Absolute)
           .setX(0d)
           .setY(0d)
           .setWidth(widthMm)
           .setHeight(heightMm);
        vPage.add(img);

        renderTextBlocks(vPage, textBlocks, String.valueOf(pageNumber));

        ofdDoc.addVPage(vPage);
    }

    /**
     * Finalize and save a multi-page OFD document, then clean up temp files.
     */
    public void closeMultiPage() throws Exception {
        try {
            if (ofdDoc != null) ofdDoc.close();
        } finally {
            // Clean up temp images
            if (multiPageTempImages != null) {
                for (Path tempImage : multiPageTempImages) {
                    if (!Files.deleteIfExists(tempImage)) {
                        log.warn("Failed to delete temp image: {}", tempImage);
                    }
                }
            }
            if (multiPageTempDir != null && !Files.deleteIfExists(multiPageTempDir)) {
                log.warn("Failed to delete temp directory: {}", multiPageTempDir);
            }
            ofdDoc = null;
            multiPageTempDir = null;
            multiPageTempImages = null;
            multiPageOutputFile = null;
        }
    }

    private OFDDoc ofdDoc;
    private Path multiPageTempDir;
    private List<Path> multiPageTempImages;
    private File multiPageOutputFile;

    public void generateOfd(BufferedImage image, List<OcrService.TextBlock> textBlocks, File outputFile) throws Exception {

        // 臨時保存圖片
        Path tempDir = Files.createTempDirectory("ofd_");
        Path tempImage = tempDir.resolve("page.png");

        try {
            ImageIO.write(image, "PNG", tempImage.toFile());

            try (OFDDoc ofdDoc = new OFDDoc(outputFile.toPath())) {

                // 轉換坐標：像素 -> mm
                double widthMm = image.getWidth() * MM_PER_INCH / ASSUMED_DPI;
                double heightMm = image.getHeight() * MM_PER_INCH / ASSUMED_DPI;

                // 創建頁面佈局
                PageLayout pageLayout = new PageLayout(widthMm, heightMm);
                pageLayout.setMargin(0d);

                // 創建虛擬頁面
                VirtualPage vPage = new VirtualPage(pageLayout);

                // 添加背景圖片
                Img img = new Img(tempImage);
                img.setPosition(Position.Absolute)
                   .setX(0d)
                   .setY(0d)
                   .setWidth(widthMm)
                   .setHeight(heightMm);
                vPage.add(img);

                // 添加不可見文字層
                renderTextBlocks(vPage, textBlocks, null);

                // 添加頁面
                ofdDoc.addVPage(vPage);
            }

        } finally {
            // 清理臨時文件
            if (!Files.deleteIfExists(tempImage)) {
                log.warn("Failed to delete temp image: {}", tempImage);
            }
            if (!Files.deleteIfExists(tempDir)) {
                log.warn("Failed to delete temp directory: {}", tempDir);
            }
        }
    }

    /**
     * Render OCR text blocks onto a virtual page using per-character absolute positioning.
     * Shared by generateOfd() and generateMultiPageOfd().
     *
     * @param vPage      the virtual page to render onto
     * @param textBlocks OCR text blocks with bounding box coordinates (pixels)
     * @param pageLabel  optional label for error messages (null for single-page mode)
     */
    private void renderTextBlocks(VirtualPage vPage, List<OcrService.TextBlock> textBlocks, String pageLabel) {
        for (OcrService.TextBlock block : textBlocks) {
            try {
                // 1. Trim invisible whitespace from OCR text
                String text = block.text.trim();
                if (text == null || text.isEmpty()) continue;

                // 2. Convert OCR bounding box: pixels -> mm
                double ocrX = block.x * MM_PER_INCH / ASSUMED_DPI;
                double ocrY = block.y * MM_PER_INCH / ASSUMED_DPI;
                double ocrW = block.width * MM_PER_INCH / ASSUMED_DPI;
                double ocrH = block.height * MM_PER_INCH / ASSUMED_DPI;

                // 3. Scale font size relative to OCR box height
                double fontSizeMm = ocrH * FONT_SIZE_SCALE;
                float fontSizePt = (float) (fontSizeMm * ASSUMED_DPI / MM_PER_INCH);

                // 4. Use SERIF font
                java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
                    .deriveFont(fontSizePt);
                java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);

                // 5. Calculate Y position using precise baseline formula
                double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
                double ascentMm = ascentPt * MM_PER_INCH / ASSUMED_DPI;
                double paragraphY = (ocrY + (ocrH * TEXT_Y_BASELINE_RATIO)) - ascentMm - (ocrH * TEXT_Y_OFFSET_RATIO);

                // 6. Per-character absolute positioning: measure each character width
                double[] charWidthsMm = new double[text.length()];
                double totalAwtWidthMm = 0;

                for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                    String singleChar = String.valueOf(text.charAt(charIdx));
                    double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();

                    // Handle zero-width space characters
                    if (singleChar.equals(" ") && wPt == 0) {
                        wPt = fontSizePt * SPACE_CHAR_WIDTH_RATIO;
                    }

                    double wMm = wPt * MM_PER_INCH / ASSUMED_DPI;
                    charWidthsMm[charIdx] = wMm;
                    totalAwtWidthMm += wMm;
                }

                // 7. Calculate horizontal scale factor to fit OCR bounding box
                double scaleX = 1.0;
                if (totalAwtWidthMm > 0) {
                    scaleX = ocrW / totalAwtWidthMm;
                }

                // 8. Draw each character at computed position
                double currentX = ocrX;

                for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                    String singleChar = String.valueOf(text.charAt(charIdx));

                    Span span = new Span(singleChar);
                    span.setFontSize(fontSizeMm);
                    span.setColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());

                    Paragraph p = new Paragraph();
                    p.add(span);
                    p.setPosition(Position.Absolute);
                    p.setMargin(0d);
                    p.setPadding(0d);
                    p.setLineSpace(0d);
                    p.setWidth(charWidthsMm[charIdx] * scaleX + PARAGRAPH_MIN_WIDTH_MM); // 確保不換行

                    p.setX(currentX);
                    p.setY(paragraphY);
                    p.setOpacity(config.getTextLayerOpacity());

                    vPage.add(p);

                    // 坐標推進
                    currentX += (charWidthsMm[charIdx] * scaleX);
                }

            } catch (Exception e) {
                String errMsg = (pageLabel != null)
                    ? "    Page " + pageLabel + " - Error drawing text: " + e.getMessage()
                    : "    Error drawing text: " + e.getMessage();
                log.error(errMsg);
            }
        }
    }
}
