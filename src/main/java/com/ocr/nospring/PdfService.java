     1|     1|package com.ocr.nospring;
     2|     2|
     3|     3|import org.apache.pdfbox.pdmodel.PDDocument;
     4|     4|import org.apache.pdfbox.pdmodel.PDPage;
     5|     5|import org.apache.pdfbox.pdmodel.PDPageContentStream;
     6|     6|import org.apache.pdfbox.pdmodel.common.PDRectangle;
     7|     7|import org.apache.pdfbox.pdmodel.font.PDFont;
     8|     8|import org.apache.pdfbox.util.Matrix;
     9|     9|import org.apache.pdfbox.pdmodel.font.PDType0Font;
    10|    10|import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
    11|    11|import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
    12|    12|import org.slf4j.Logger;
    13|    13|import org.slf4j.LoggerFactory;
    14|    14|import javax.imageio.ImageIO;
    15|    15|import java.awt.image.BufferedImage;
    16|    16|import java.io.ByteArrayOutputStream;
    17|    17|import java.io.File;
    18|    18|import java.util.ArrayList;
    19|    19|import java.util.List;
    20|    20|
    21|    21|/**
    22|    22| * PDF 服務 - 無 Spring Boot（使用與 OFD 相同的逐字符定位算法）
    23|    23| */
    24|    24|public class PdfService {
    25|    25|
    26|    26|    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    27|    27|
    28|    28|    private final Config config;
    29|    29|
    30|    30|    public PdfService(Config config) {
    31|    31|        this.config = config;
    32|    32|    }
    33|    33|
    34|    34|    public void generatePdf(BufferedImage image, List<TextBlock> textBlocks, File outputFile) throws Exception {
    35|    35|
    36|    36|        try (PDDocument document = new PDDocument()) {
    37|    37|            // 載入字體
    38|    38|            PDFont font = loadFont(document);
    39|    39|
    40|    40|            // 建立頁面
    41|    41|            float width = image.getWidth();
    42|    42|            float height = image.getHeight();
    43|    43|            PDPage page = new PDPage(new PDRectangle(width, height));
    44|    44|            document.addPage(page);
    45|    45|
    46|    46|            // 轉換圖片
    47|    47|            ByteArrayOutputStream baos = new ByteArrayOutputStream();
    48|    48|            ImageIO.write(image, "PNG", baos);
    49|    49|            byte[] imageBytes = baos.toByteArray();
    50|    50|
    51|    51|            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
    52|    52|                document, imageBytes, "image"
    53|    53|            );
    54|    54|
    55|    55|            // 繪製內容
    56|    56|            try (PDPageContentStream contentStream = new PDPageContentStream(
    57|    57|                document, page,
    58|    58|                PDPageContentStream.AppendMode.APPEND,
    59|    59|                true,
    60|    60|                true
    61|    61|            )) {
    62|    62|                // 1. 繪製圖片
    63|    63|                contentStream.drawImage(pdImage, 0, 0, width, height);
    64|    64|
    65|    65|                // 2. 繪製透明文字層（使用與 OFD 相同的算法）
    66|    66|                drawTransparentTextLayer(contentStream, textBlocks, font, width, height);
    67|    67|            }
    68|    68|
    69|    69|            // 保存
    70|    70|            document.save(outputFile);
    71|    71|        }
    72|    72|    }
    73|    73|
    74|    74|    /**
    75|    75|     * 生成多頁 PDF
    76|    76|     */
    77|    77|    public void generateMultiPagePdf(List<BufferedImage> images, List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {
    78|    78|
    79|    79|        if (images.size() != allTextBlocks.size()) {
    80|    80|            throw new IllegalArgumentException("Images and text blocks count mismatch");
    81|    81|        }
    82|    82|
    83|    83|        try (PDDocument document = new PDDocument()) {
    84|    84|            // 載入字體
    85|    85|            PDFont font = loadFont(document);
    86|    86|
    87|    87|            // 處理每一頁
    88|    88|            for (int pageIndex = 0; pageIndex < images.size(); pageIndex++) {
    89|    89|                BufferedImage image = images.get(pageIndex);
    90|    90|                List<TextBlock> textBlocks = allTextBlocks.get(pageIndex);
    91|    91|
    92|    92|                // 建立頁面
    93|    93|                float width = image.getWidth();
    94|    94|                float height = image.getHeight();
    95|    95|                PDPage page = new PDPage(new PDRectangle(width, height));
    96|    96|                document.addPage(page);
    97|    97|
    98|    98|                // 轉換圖片
    99|    99|                ByteArrayOutputStream baos = new ByteArrayOutputStream();
   100|   100|                ImageIO.write(image, "PNG", baos);
   101|   101|                byte[] imageBytes = baos.toByteArray();
   102|   102|
   103|   103|                PDImageXObject pdImage = PDImageXObject.createFromByteArray(
   104|   104|                    document, imageBytes, "image"
   105|   105|                );
   106|   106|
   107|   107|                // 繪製內容
   108|   108|                try (PDPageContentStream contentStream = new PDPageContentStream(
   109|   109|                    document, page,
   110|   110|                    PDPageContentStream.AppendMode.APPEND,
   111|   111|                    true,
   112|   112|                    true
   113|   113|                )) {
   114|   114|                    // 1. 繪製圖片
   115|   115|                    contentStream.drawImage(pdImage, 0, 0, width, height);
   116|   116|
   117|   117|                    // 2. 繪製透明文字層（使用與 OFD 相同的算法）
   118|   118|                    drawTransparentTextLayer(contentStream, textBlocks, font, width, height);
   119|   119|                }
   120|   120|            }
   121|   121|
   122|   122|            // 保存
   123|   123|            document.save(outputFile);
   124|   124|        }
   125|   125|    }
   126|   126|
   127|   127|    /**
   128|   128|     * 繪製透明文字層（整段定位，不用逐字/scaleX）
   129|   129|     */
   130|   130|    private void drawTransparentTextLayer(PDPageContentStream contentStream, List<TextBlock> textBlocks, PDFont font, float width, float height) throws Exception {
   131|   131|        // 設置透明度
   132|   132|        org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState extGState = new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
   133|   133|        float opacity = (float) config.getTextLayerOpacity();
   134|   134|        extGState.setNonStrokingAlphaConstant(opacity);
   135|   135|        extGState.setStrokingAlphaConstant(opacity);
   136|   136|        contentStream.setGraphicsStateParameters(extGState);
   137|   137|
   138|   138|        // 設置顏色
   139|   139|        contentStream.setRenderingMode(RenderingMode.FILL);
   140|   140|        contentStream.setNonStrokingColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());
   141|   141|
   142|   142|        contentStream.beginText();
   143|   143|
   144|   144|        for (TextBlock block : textBlocks) {
   145|   145|            try {
   146|   146|                String text = block.getText().trim();
   147|   147|                if (text == null || text.isEmpty()) continue;
   148|   148|
   149|   149|                double ocrX = block.getX();
   150|   150|                double ocrY = block.getY();
   151|   151|                double ocrW = block.getWidth();
   152|   152|                double ocrH = block.getHeight();
   153|   153|
   154|   154|                // fontSize = box 高度
   155|   155|                float fontSizePt = (float) ocrH;
   156|   156|
   157|   157|                // 計算文字自然寬度
   158|   158|                float textWidth = font.getStringWidth(text) / 1000f * fontSizePt;
   159|   159|
   160|   160|                // 如果太寬就縮小 fontSize
   161|   161|                if (textWidth > ocrW) {
   162|   162|                    fontSizePt = (float) (fontSizePt * (ocrW / textWidth));
   163|   163|                    textWidth = font.getStringWidth(text) / 1000f * fontSizePt;
   164|   164|                }
   165|   165|
   166|   166|                // Y: 文字底部往上抬一點（約 0.1 * fontSize）
   167|   167|                float pdfY = (float) (height - ocrY - ocrH + ocrH * 0.1);
   168|   168|
   169|   169|                // 判斷直列文字
   170|   170|                boolean isVertical = ocrH > ocrW * 1.5;
   171|   171|
   172|   172|                if (isVertical) {
   173|   173|                    // 直列：逐字從上到下
   174|   174|                    for (int i = 0; i < text.length(); i++) {
   175|   175|                        String ch = String.valueOf(text.charAt(i));
   176|   176|                        try {
   177|   177|                            float chW = font.getStringWidth(ch) / 1000f * fontSizePt;
   178|   178|                            float chX = (float) (ocrX + (ocrW - chW) / 2);
   179|   179|                            float chY = (float) (height - ocrY - fontSizePt * (i + 1));
   180|   180|                            contentStream.setFont(font, fontSizePt);
   181|   181|                            contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, chX, chY));
   182|   182|                            contentStream.showText(ch);
   183|   183|                        } catch (Exception e) {
   184|   184|                            log.warn("    [WARN] Skip char '{}': {}", ch, e.getMessage());
   185|   185|                        }
   186|   186|                    }
   187|   187|                } else {
   188|   188|                    // 橫列：整段定位，左對齊
   189|   189|                    contentStream.setFont(font, fontSizePt);
   190|   190|                    contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, (float) ocrX, pdfY));
   191|   191|                    contentStream.showText(text);
   192|   192|                }
   193|   193|            } catch (Exception e) {
   194|   194|                log.error("    Error drawing text: {}", e.getMessage());
   195|   195|            }
   196|   196|        }
   197|   197|
   198|   198|        contentStream.endText();
   199|   199|    }
   200|   200|
   201|   201|    /**
   202|   202|     * 載入字體 (per architecture rule: GoNotoKurrent primary, wqy-ZenHei fallback)
   203|   203|     */
   204|   204|    private PDFont loadFont(PDDocument document) throws Exception {
   205|   205|        String fontPath = config.getFontPath();
   206|   206|
   207|   207|        // 1. 嘗試配置的字體（RTL 語言時跳過預設字型，改用 RTL 專用字型）
   208|   208|        String ocrLang = config.getOcrLanguage();
   209|   209|        boolean isRTL = ocrLang != null && (ocrLang.equals("he") || ocrLang.startsWith("ar") || ocrLang.equals("fa") || ocrLang.equals("ur"));
   210|   210|
   211|   211|        if (fontPath != null && new File(fontPath).exists() && !isRTL) {
   212|   212|            try {
   213|   213|                PDFont font = PDType0Font.load(document, new File(fontPath));
   214|   214|                log.info("    Loaded font (config): {}", fontPath);
   215|   215|                return font;
   216|   216|            } catch (Exception e) {
   217|   217|                log.warn("    Warning: Cannot load font from {}: {}", fontPath, e.getMessage());
   218|   218|            }
   219|   219|        }
   220|   220|
   221|   221|        // 2. RTL 語言（希伯來文、阿拉伯文等）— 跨平台字體偵測
   222|   222|        if (isRTL) {
   223|   223|            String[] rtlFontNames = {"tahoma.ttf", "segoeui.ttf", "DejaVuSans.ttf", "Arial.ttf"};
   224|   224|            for (String fontDir : getSystemFontDirectories()) {
   225|   225|                for (String fontName : rtlFontNames) {
   226|   226|                    File fontFile = new File(fontDir, fontName);
   227|   227|                    if (fontFile.exists()) {
   228|   228|                        try {
   229|   229|                            PDFont font = PDType0Font.load(document, fontFile);
   230|   230|                            log.info("    Loaded font (RTL): {}", fontFile.getAbsolutePath());
   231|   231|                            return font;
   232|   232|                        } catch (Exception e) {
   233|   233|                            // skip unsupported font format
   234|   234|                        }
   235|   235|                    }
   236|   236|                }
   237|   237|            }
   238|   238|        }
   239|   239|
   240|   240|        // 3. Primary: GoNotoKurrent-Regular.ttf (per architecture rule)
   241|   241|        String[] primaryFonts = {
   242|   242|            "fonts/GoNotoKurrent-Regular.ttf",
   243|   243|        };
   244|   244|        for (String path : primaryFonts) {
   245|   245|            File fontFile = new File(path);
   246|   246|            if (fontFile.exists()) {
   247|   247|                try {
   248|   248|                    PDFont font = PDType0Font.load(document, fontFile);
   249|   249|                    log.info("    Loaded font (GoNotoKurrent): {}", path);
   250|   250|                    return font;
   251|   251|                } catch (Exception e) {
   252|   252|                }
   253|   253|            }
   254|   254|        }
   255|   255|
   256|   256|        // 4. Fallback: wqy-ZenHei.ttf (per architecture rule)
   257|   257|        String[] fallbackFonts = {
   258|   258|            "fonts/wqy-ZenHei.ttf",
   259|   259|        };
   260|   260|        for (String path : fallbackFonts) {
   261|   261|            File fontFile = new File(path);
   262|   262|            if (fontFile.exists()) {
   263|   263|                try {
   264|   264|                    PDFont font = PDType0Font.load(document, fontFile);
   265|   265|                    log.info("    Loaded font (wqy-ZenHei fallback): {}", path);
   266|   266|                    return font;
   267|   267|                } catch (Exception e) {
   268|   268|                }
   269|   269|            }
   270|   270|        }
   271|   271|
   272|   272|        // 5. 最後使用默認字體（僅支持英文）
   273|   273|        log.warn("    Warning: Using default Helvetica font (English only)");
   274|   274|        return org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
   275|   275|    }
   276|   276|
   277|   277|    /**
   278|   278|     * 取得系統字體目錄（跨平台）
   279|   279|     */
   280|   280|    private String[] getSystemFontDirectories() {
   281|   281|        List<String> dirs = new ArrayList<>();
   282|   282|        String os = System.getProperty("os.name", "").toLowerCase();
   283|   283|        if (os.contains("win")) {
   284|   284|            String winDir = System.getenv("WINDIR");
   285|   285|            if (winDir != null) dirs.add(winDir + "\\Fonts");
   286|   286|        } else if (os.contains("mac")) {
   287|   287|            dirs.add("/System/Library/Fonts");
   288|   288|            dirs.add("/System/Library/Fonts/Supplemental");
   289|   289|            dirs.add("/Library/Fonts");
   290|   290|        } else {
   291|   291|            dirs.add("/usr/share/fonts/truetype");
   292|   292|            dirs.add("/usr/share/fonts/TTF");
   293|   293|            dirs.add("/usr/local/share/fonts");
   294|   294|        }
   295|   295|        return dirs.toArray(new String[0]);
   296|   296|    }
   297|   297|}
   298|   298|