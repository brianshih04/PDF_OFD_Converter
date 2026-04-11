     1|     1|package com.ocr.nospring;
     2|     2|
     3|     3|import org.ofdrw.layout.OFDDoc;
     4|     4|import org.ofdrw.layout.PageLayout;
     5|     5|import org.ofdrw.layout.VirtualPage;
     6|     6|import org.ofdrw.layout.element.Img;
     7|     7|import org.ofdrw.layout.element.Paragraph;
     8|     8|import org.ofdrw.layout.element.Span;
     9|     9|import org.ofdrw.layout.element.Position;
    10|    10|import org.slf4j.Logger;
    11|    11|import org.slf4j.LoggerFactory;
    12|    12|import javax.imageio.ImageIO;
    13|    13|import java.awt.image.BufferedImage;
    14|    14|import java.io.File;
    15|    15|import java.nio.file.Files;
    16|    16|import java.nio.file.Path;
    17|    17|import java.util.ArrayList;
    18|    18|import java.util.List;
    19|    19|
    20|    20|/**
    21|    21| * OFD 服務 - 無 Spring Boot
    22|    22| */
    23|    23|public class OfdService {
    24|    24|
    25|    25|    private static final Logger log = LoggerFactory.getLogger(OfdService.class);
    26|    26|    
    27|    27|    private final Config config;
    28|    28|    
    29|    29|    public OfdService(Config config) {
    30|    30|        this.config = config;
    31|    31|    }
    32|    32|    
    33|    33|    /**
    34|    34|     * 生成多頁 OFD
    35|    35|     */
    36|    36|    public void generateMultiPageOfd(List<BufferedImage> images, List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {
    37|    37|        
    38|    38|        if (images.size() != allTextBlocks.size()) {
    39|    39|            throw new IllegalArgumentException("Images and text blocks count mismatch");
    40|    40|        }
    41|    41|        
    42|    42|        // 臨時保存所有圖片
    43|    43|        Path tempDir = Files.createTempDirectory("ofd_multipage_");
    44|    44|        List<Path> tempImages = new ArrayList<>();
    45|    45|        
    46|    46|        try {
    47|    47|            try (OFDDoc ofdDoc = new OFDDoc(outputFile.toPath())) {
    48|    48|                
    49|    49|                // 處理每一頁
    50|    50|                for (int pageIndex = 0; pageIndex < images.size(); pageIndex++) {
    51|    51|                    BufferedImage image = images.get(pageIndex);
    52|    52|                    List<TextBlock> textBlocks = allTextBlocks.get(pageIndex);
    53|    53|                    
    54|    54|                    // 保存圖片
    55|    55|                    Path tempImage = tempDir.resolve("page_" + pageIndex + ".png");
    56|    56|                    ImageIO.write(image, "PNG", tempImage.toFile());
    57|    57|                    tempImages.add(tempImage); // 記錄所有臨時圖片
    58|    58|                    
    59|    59|                    // 轉換坐標：像素 -> mm (假設 DPI = 72)
    60|    60|                    double widthMm = image.getWidth() * 25.4 / 72.0;
    61|    61|                    double heightMm = image.getHeight() * 25.4 / 72.0;
    62|    62|                    
    63|    63|                    // 創建頁面佈局
    64|    64|                    PageLayout pageLayout = new PageLayout(widthMm, heightMm);
    65|    65|                    pageLayout.setMargin(0d);
    66|    66|                    
    67|    67|                    // 創建虛擬頁面
    68|    68|                    VirtualPage vPage = new VirtualPage(pageLayout);
    69|    69|                    
    70|    70|                    // 添加背景圖片
    71|    71|                    Img img = new Img(tempImage);
    72|    72|                    img.setPosition(Position.Absolute)
    73|    73|                       .setX(0d)
    74|    74|                       .setY(0d)
    75|    75|                       .setWidth(widthMm)
    76|    76|                       .setHeight(heightMm);
    77|    77|                    vPage.add(img);
    78|    78|                    
    79|    79|                    // 添加不可見文字層（使用終極算法：逐字符絕對定位 + AWT 字體寬度計算）
    80|    80|                    for (TextBlock block : textBlocks) {
    81|    81|                        try {
    82|    82|                            // 1. 去除 OCR 文字頭尾的隱形空白
    83|    83|                            String text = block.getText().trim();
    84|    84|                            if (text == null || text.isEmpty()) continue;
    85|    85|                            
    86|    86|                            // 2. OCR 邊界框
    87|    87|                            double ocrX = block.getX() * 25.4 / 72.0;
    88|    88|                            double ocrY = block.getY() * 25.4 / 72.0;
    89|    89|                            double ocrW = block.getWidth() * 25.4 / 72.0;
    90|    90|                            double ocrH = block.getHeight() * 25.4 / 72.0;
    91|    91|                            
    92|    92|                            // 3. 字號保持 0.75 完美比例
    93|    93|                            double fontSizeMm = ocrH * 0.75;
    94|    94|                            float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);
    95|    95|                            
    96|    96|                            // 4. 使用 SERIF 字體
    97|    97|                            java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
    98|    98|                                .deriveFont(fontSizePt);
    99|    99|                            java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
   100|   100|                            
   101|   101|                            // 5. Y 軸使用精確公式（往上移動 0.1 字高）
   102|   102|                            double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
   103|   103|                            double ascentMm = ascentPt * 25.4 / 72.0;
   104|   104|                            double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
   105|   105|                            
   106|   106|                            // 6. 終極算法：逐字符絕對定位
   107|   107|                            double[] charWidthsMm = new double[text.length()];
   108|   108|                            double totalAwtWidthMm = 0;
   109|   109|                            
   110|   110|                            for (int charIdx = 0; charIdx < text.length(); charIdx++) {
   111|   111|                                String singleChar = String.valueOf(text.charAt(charIdx));
   112|   112|                                double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
   113|   113|                                
   114|   114|                                // 處理空白字符
   115|   115|                                if (singleChar.equals(" ") && wPt == 0) {
   116|   116|                                    wPt = fontSizePt * 0.3;
   117|   117|                                }
   118|   118|                                
   119|   119|                                double wMm = wPt * 25.4 / 72.0;
   120|   120|                                charWidthsMm[charIdx] = wMm;
   121|   121|                                totalAwtWidthMm += wMm;
   122|   122|                            }
   123|   123|                            
   124|   124|                            // 7. 計算縮放比例
   125|   125|                            double scaleX = 1.0;
   126|   126|                            if (totalAwtWidthMm > 0) {
   127|   127|                                scaleX = ocrW / totalAwtWidthMm;
   128|   128|                            }
   129|   129|                            
   130|   130|                            // 8. 逐字符繪製
   131|   131|                            double currentX = ocrX;
   132|   132|                            
   133|   133|                            for (int charIdx = 0; charIdx < text.length(); charIdx++) {
   134|   134|                                String singleChar = String.valueOf(text.charAt(charIdx));
   135|   135|                                
   136|   136|                                Span span = new Span(singleChar);
   137|   137|                                span.setFontSize(fontSizeMm);
   138|   138|                                span.setColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());
   139|   139|                                
   140|   140|                                Paragraph p = new Paragraph();
   141|   141|                                p.add(span);
   142|   142|                                p.setPosition(Position.Absolute);
   143|   143|                                p.setMargin(0d);
   144|   144|                                p.setPadding(0d);
   145|   145|                                p.setLineSpace(0d);
   146|   146|                                p.setWidth(charWidthsMm[charIdx] * scaleX + 10.0); // 確保不換行
   147|   147|                                
   148|   148|                                // 強制指定 X 與 Y
   149|   149|                                p.setX(currentX);
   150|   150|                                p.setY(paragraphY);
   151|   151|                                
   152|   152|                                // 從配置讀取透明度
   153|   153|                                p.setOpacity(config.getTextLayerOpacity());
   154|   154|                                
   155|   155|                                vPage.add(p);
   156|   156|                                
   157|   157|                                // 坐標推進
   158|   158|                                currentX += (charWidthsMm[charIdx] * scaleX);
   159|   159|                            }
   160|   160|                            
   161|   161|                        } catch (Exception e) {
   162|   162|                            log.error("    Page {} - Error drawing text: {}", pageIndex + 1, e.getMessage());
   163|   163|                        }
   164|   164|                    }
   165|   165|                    
   166|   166|                    // 添加頁面（不刪除圖片！）
   167|   167|                    ofdDoc.addVPage(vPage);
   168|   168|                }
   169|   169|            }
   170|   170|            // OFD 文檔已在此處關閉並生成完成
   171|   171|            
   172|   172|        } finally {
   173|   173|            // 在文檔完全生成後，再清理所有臨時圖片
   174|   174|            for (Path tempImage : tempImages) {
   175|   175|                if (!Files.deleteIfExists(tempImage)) {
   176|   176|                    log.warn("Failed to delete temp image: {}", tempImage);
   177|   177|                }
   178|   178|            }
   179|   179|            if (!Files.deleteIfExists(tempDir)) {
   180|   180|                log.warn("Failed to delete temp directory: {}", tempDir);
   181|   181|            }
   182|   182|        }
   183|   183|    }
   184|   184|    
   185|   185|    public void generateOfd(BufferedImage image, List<TextBlock> textBlocks, File outputFile) throws Exception {
   186|   186|        
   187|   187|        // 臨時保存圖片
   188|   188|        Path tempDir = Files.createTempDirectory("ofd_");
   189|   189|        Path tempImage = tempDir.resolve("page.png");
   190|   190|        ImageIO.write(image, "PNG", tempImage.toFile());
   191|   191|        
   192|   192|        try (OFDDoc ofdDoc = new OFDDoc(outputFile.toPath())) {
   193|   193|            
   194|   194|            // 轉換坐標：像素 -> mm (假設 DPI = 72)
   195|   195|            double widthMm = image.getWidth() * 25.4 / 72.0;
   196|   196|            double heightMm = image.getHeight() * 25.4 / 72.0;
   197|   197|            
   198|   198|            // 創建頁面佈局
   199|   199|            PageLayout pageLayout = new PageLayout(widthMm, heightMm);
   200|   200|            pageLayout.setMargin(0d);
   201|   201|            
   202|   202|            // 創建虛擬頁面
   203|   203|            VirtualPage vPage = new VirtualPage(pageLayout);
   204|   204|            
   205|   205|            // 添加背景圖片
   206|   206|            Img img = new Img(tempImage);
   207|   207|            img.setPosition(Position.Absolute)
   208|   208|               .setX(0d)
   209|   209|               .setY(0d)
   210|   210|               .setWidth(widthMm)
   211|   211|               .setHeight(heightMm);
   212|   212|            vPage.add(img);
   213|   213|            
   214|   214|            // 添加不可見文字層（使用終極算法：逐字符絕對定位 + AWT 字體寬度計算）
   215|   215|            for (TextBlock block : textBlocks) {
   216|   216|                try {
   217|   217|                    // 1. 去除 OCR 文字頭尾的隱形空白
   218|   218|                    String text = block.getText().trim();
   219|   219|                    if (text == null || text.isEmpty()) continue;
   220|   220|                    
   221|   221|                    // 2. OCR 邊界框
   222|   222|                    double ocrX = block.getX() * 25.4 / 72.0;
   223|   223|                    double ocrY = block.getY() * 25.4 / 72.0;
   224|   224|                    double ocrW = block.getWidth() * 25.4 / 72.0;
   225|   225|                    double ocrH = block.getHeight() * 25.4 / 72.0;
   226|   226|                    
   227|   227|                    // 3. 字號保持 0.75 完美比例
   228|   228|                    double fontSizeMm = ocrH * 0.75;
   229|   229|                    float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);
   230|   230|                    
   231|   231|                    // 4. 使用 SERIF 字體
   232|   232|                    java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
   233|   233|                        .deriveFont(fontSizePt);
   234|   234|                    java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
   235|   235|                    
   236|   236|                    // 5. Y 軸使用精確公式（往上移動 0.1 字高）
   237|   237|                    double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
   238|   238|                    double ascentMm = ascentPt * 25.4 / 72.0;
   239|   239|                    double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
   240|   240|                    
   241|   241|                    // 6. 終極算法：逐字符絕對定位
   242|   242|                    double[] charWidthsMm = new double[text.length()];
   243|   243|                    double totalAwtWidthMm = 0;
   244|   244|                    
   245|   245|                    for (int charIdx = 0; charIdx < text.length(); charIdx++) {
   246|   246|                        String singleChar = String.valueOf(text.charAt(charIdx));
   247|   247|                        double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
   248|   248|                        
   249|   249|                        // 處理空白字符
   250|   250|                        if (singleChar.equals(" ") && wPt == 0) {
   251|   251|                            wPt = fontSizePt * 0.3;
   252|   252|                        }
   253|   253|                        
   254|   254|                        double wMm = wPt * 25.4 / 72.0;
   255|   255|                        charWidthsMm[charIdx] = wMm;
   256|   256|                        totalAwtWidthMm += wMm;
   257|   257|                    }
   258|   258|                    
   259|   259|                    // 7. 計算縮放比例
   260|   260|                    double scaleX = 1.0;
   261|   261|                    if (totalAwtWidthMm > 0) {
   262|   262|                        scaleX = ocrW / totalAwtWidthMm;
   263|   263|                    }
   264|   264|                    
   265|   265|                    // 8. 逐字符繪製
   266|   266|                    double currentX = ocrX;
   267|   267|                    
   268|   268|                    for (int charIdx = 0; charIdx < text.length(); charIdx++) {
   269|   269|                        String singleChar = String.valueOf(text.charAt(charIdx));
   270|   270|                        
   271|   271|                        Span span = new Span(singleChar);
   272|   272|                        span.setFontSize(fontSizeMm);
   273|   273|                        span.setColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());
   274|   274|                        
   275|   275|                        Paragraph p = new Paragraph();
   276|   276|                        p.add(span);
   277|   277|                        p.setPosition(Position.Absolute);
   278|   278|                        p.setMargin(0d);
   279|   279|                        p.setPadding(0d);
   280|   280|                        p.setLineSpace(0d);
   281|   281|                        p.setWidth(charWidthsMm[charIdx] * scaleX + 10.0); // 確保不換行
   282|   282|                        
   283|   283|                        // 強制指定 X 與 Y
   284|   284|                        p.setX(currentX);
   285|   285|                        p.setY(paragraphY);
   286|   286|                        
   287|   287|                        // 從配置讀取透明度
   288|   288|                        p.setOpacity(config.getTextLayerOpacity());
   289|   289|                        
   290|   290|                        vPage.add(p);
   291|   291|                        
   292|   292|                        // 坐標推進
   293|   293|                        currentX += (charWidthsMm[charIdx] * scaleX);
   294|   294|                    }
   295|   295|                    
   296|   296|                } catch (Exception e) {
   297|   297|                    log.error("    Error drawing text: {}", e.getMessage());
   298|   298|                }
   299|   299|            }
   300|   300|            
   301|   301|            // 添加頁面
   302|   302|            ofdDoc.addVPage(vPage);
   303|   303|        }
   304|   304|        
   305|   305|        // 清理臨時文件
   306|   306|        if (!Files.deleteIfExists(tempImage)) {
   307|   307|            log.warn("Failed to delete temp image: {}", tempImage);
   308|   308|        }
   309|   309|        if (!Files.deleteIfExists(tempDir)) {
   310|   310|            log.warn("Failed to delete temp directory: {}", tempDir);
   311|   311|        }
   312|   312|    }
   313|   313|}
   314|   314|