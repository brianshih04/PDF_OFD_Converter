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
    
    private final Config config;
    
    public OfdService(Config config) {
        this.config = config;
    }
    
    /**
     * 生成多頁 OFD
     */
    public void generateMultiPageOfd(List<BufferedImage> images, List<List<OcrService.TextBlock>> allTextBlocks, File outputFile) throws Exception {
        
        if (images.size() != allTextBlocks.size()) {
            throw new IllegalArgumentException("Images and text blocks count mismatch");
        }
        
        // 臨時保存所有圖片
        Path tempDir = Files.createTempDirectory("ofd_multipage_");
        List<Path> tempImages = new ArrayList<>();
        
        try {
            try (OFDDoc ofdDoc = new OFDDoc(outputFile.toPath())) {
                
                // 處理每一頁
                for (int pageIndex = 0; pageIndex < images.size(); pageIndex++) {
                    BufferedImage image = images.get(pageIndex);
                    List<OcrService.TextBlock> textBlocks = allTextBlocks.get(pageIndex);
                    
                    // 保存圖片
                    Path tempImage = tempDir.resolve("page_" + pageIndex + ".png");
                    ImageIO.write(image, "PNG", tempImage.toFile());
                    tempImages.add(tempImage); // 記錄所有臨時圖片
                    
                    // 轉換坐標：像素 -> mm (假設 DPI = 72)
                    double widthMm = image.getWidth() * 25.4 / 72.0;
                    double heightMm = image.getHeight() * 25.4 / 72.0;
                    
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
                    
                    // 添加不可見文字層（使用終極算法：逐字符絕對定位 + AWT 字體寬度計算）
                    for (OcrService.TextBlock block : textBlocks) {
                        try {
                            // 1. 去除 OCR 文字頭尾的隱形空白
                            String text = block.text.trim();
                            if (text == null || text.isEmpty()) continue;
                            
                            // 2. OCR 邊界框
                            double ocrX = block.x * 25.4 / 72.0;
                            double ocrY = block.y * 25.4 / 72.0;
                            double ocrW = block.width * 25.4 / 72.0;
                            double ocrH = block.height * 25.4 / 72.0;
                            
                            // 3. 字號保持 0.75 完美比例
                            double fontSizeMm = ocrH * 0.75;
                            float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);
                            
                            // 4. 使用 SERIF 字體
                            java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
                                .deriveFont(fontSizePt);
                            java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
                            
                            // 5. Y 軸使用精確公式（往上移動 0.1 字高）
                            double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
                            double ascentMm = ascentPt * 25.4 / 72.0;
                            double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
                            
                            // 6. 終極算法：逐字符絕對定位
                            double[] charWidthsMm = new double[text.length()];
                            double totalAwtWidthMm = 0;
                            
                            for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                                String singleChar = String.valueOf(text.charAt(charIdx));
                                double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
                                
                                // 處理空白字符
                                if (singleChar.equals(" ") && wPt == 0) {
                                    wPt = fontSizePt * 0.3;
                                }
                                
                                double wMm = wPt * 25.4 / 72.0;
                                charWidthsMm[charIdx] = wMm;
                                totalAwtWidthMm += wMm;
                            }
                            
                            // 7. 計算縮放比例
                            double scaleX = 1.0;
                            if (totalAwtWidthMm > 0) {
                                scaleX = ocrW / totalAwtWidthMm;
                            }
                            
                            // 8. 逐字符繪製
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
                                p.setWidth(charWidthsMm[charIdx] * scaleX + 10.0); // 確保不換行
                                
                                // 強制指定 X 與 Y
                                p.setX(currentX);
                                p.setY(paragraphY);
                                
                                // 從配置讀取透明度
                                p.setOpacity(config.getTextLayerOpacity());
                                
                                vPage.add(p);
                                
                                // 坐標推進
                                currentX += (charWidthsMm[charIdx] * scaleX);
                            }
                            
                        } catch (Exception e) {
                            log.error("    Page {} - Error drawing text: {}", pageIndex + 1, e.getMessage());
                        }
                    }
                    
                    // 添加頁面（不刪除圖片！）
                    ofdDoc.addVPage(vPage);
                }
            }
            // OFD 文檔已在此處關閉並生成完成
            
        } finally {
            // 在文檔完全生成後，再清理所有臨時圖片
            for (Path tempImage : tempImages) {
                if (!Files.deleteIfExists(tempImage)) {
                    log.warn("Failed to delete temp image: {}", tempImage);
                }
            }
            if (!Files.deleteIfExists(tempDir)) {
                log.warn("Failed to delete temp directory: {}", tempDir);
            }
        }
    }
    
    public void generateOfd(BufferedImage image, List<OcrService.TextBlock> textBlocks, File outputFile) throws Exception {

        // 臨時保存圖片
        Path tempDir = Files.createTempDirectory("ofd_");
        Path tempImage = tempDir.resolve("page.png");

        try {
            ImageIO.write(image, "PNG", tempImage.toFile());

            try (OFDDoc ofdDoc = new OFDDoc(outputFile.toPath())) {

                // 轉換坐標：像素 -> mm (假設 DPI = 72)
                double widthMm = image.getWidth() * 25.4 / 72.0;
                double heightMm = image.getHeight() * 25.4 / 72.0;

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

                // 添加不可見文字層（使用終極算法：逐字符絕對定位 + AWT 字體寬度計算）
                for (OcrService.TextBlock block : textBlocks) {
                    try {
                        // 1. 去除 OCR 文字頭尾的隱形空白
                        String text = block.text.trim();
                        if (text == null || text.isEmpty()) continue;

                        // 2. OCR 邊界框
                        double ocrX = block.x * 25.4 / 72.0;
                        double ocrY = block.y * 25.4 / 72.0;
                        double ocrW = block.width * 25.4 / 72.0;
                        double ocrH = block.height * 25.4 / 72.0;

                        // 3. 字號保持 0.75 完美比例
                        double fontSizeMm = ocrH * 0.75;
                        float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);

                        // 4. 使用 SERIF 字體
                        java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
                            .deriveFont(fontSizePt);
                        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);

                        // 5. Y 軸使用精確公式（往上移動 0.1 字高）
                        double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
                        double ascentMm = ascentPt * 25.4 / 72.0;
                        double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);

                        // 6. 終極算法：逐字符絕對定位
                        double[] charWidthsMm = new double[text.length()];
                        double totalAwtWidthMm = 0;

                        for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                            String singleChar = String.valueOf(text.charAt(charIdx));
                            double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();

                            // 處理空白字符
                            if (singleChar.equals(" ") && wPt == 0) {
                                wPt = fontSizePt * 0.3;
                            }

                            double wMm = wPt * 25.4 / 72.0;
                            charWidthsMm[charIdx] = wMm;
                            totalAwtWidthMm += wMm;
                        }

                        // 7. 計算縮放比例
                        double scaleX = 1.0;
                        if (totalAwtWidthMm > 0) {
                            scaleX = ocrW / totalAwtWidthMm;
                        }

                        // 8. 逐字符繪製
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
                            p.setWidth(charWidthsMm[charIdx] * scaleX + 10.0); // 確保不換行

                            // 強制指定 X 與 Y
                            p.setX(currentX);
                            p.setY(paragraphY);

                            // 從配置讀取透明度
                            p.setOpacity(config.getTextLayerOpacity());

                            vPage.add(p);

                            // 坐標推進
                            currentX += (charWidthsMm[charIdx] * scaleX);
                        }

                    } catch (Exception e) {
                        log.error("    Error drawing text: {}", e.getMessage());
                    }
                }

                // 添加頁面
                ofdDoc.addVPage(vPage);
            }

        } finally {
            // 清理臨時文件（與 generateMultiPageOfd 保持一致的 finally 模式）
            if (!Files.deleteIfExists(tempImage)) {
                log.warn("Failed to delete temp image: {}", tempImage);
            }
            if (!Files.deleteIfExists(tempDir)) {
                log.warn("Failed to delete temp directory: {}", tempDir);
            }
        }
    }
}
