     1|     1|package com.ocr.nospring;
     2|     2|
     3|     3|import com.github.houbb.opencc4j.util.ZhConverterUtil;
     4|     4|import org.slf4j.Logger;
     5|     5|import org.slf4j.LoggerFactory;
     6|     6|import javax.imageio.ImageIO;
     7|     7|import java.awt.image.BufferedImage;
     8|     8|import java.io.File;
     9|     9|import java.util.ArrayList;
    10|    10|import java.util.Date;
    11|    11|import java.util.List;
    12|    12|
    13|    13|/**
    14|    14| * Processing service with progress callback support.
    15|    15| * Extracts processing logic from Main.java into reusable methods.
    16|    16| */
    17|    17|public class ProcessingService {
    18|    18|
    19|    19|    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);
    20|    20|
    21|    21|    /**
    22|    22|     * Callback interface for progress updates.
    23|    23|     */
    24|    24|    public interface ProgressCallback {
    25|    25|        void onProgress(int current, int total, String message);
    26|    26|        void onComplete(List<String> outputFiles);
    27|    27|        void onError(String message);
    28|    28|    }
    29|    29|
    30|    30|    private final Config config;
    31|    31|    private final OcrService ocrService;
    32|    32|    private final PdfService pdfService;
    33|    33|    private final TextService textService;
    34|    34|    private final OfdService ofdService;
    35|    35|    private TesseractOcrService tesseractService;
    36|    36|
    37|    37|    private volatile boolean cancelled = false;
    38|    38|
    39|    39|    /**
    40|    40|     * Constructor that takes Config and initializes all services.
    41|    41|     */
    42|    42|    public ProcessingService(Config config) {
    43|    43|        this.config = config;
    44|    44|        this.ocrService = new OcrService();
    45|    45|        this.pdfService = new PdfService(config);
    46|    46|        this.textService = new TextService();
    47|    47|        this.ofdService = new OfdService(config);
    48|    48|    }
    49|    49|
    50|    50|    /**
    51|    51|     * Cancel the current processing operation.
    52|    52|     */
    53|    53|    public void cancel() {
    54|    54|        this.cancelled = true;
    55|    55|    }
    56|    56|
    57|    57|    /**
    58|    58|     * Check if cancelled and throw InterruptedException if so.
    59|    59|     */
    60|    60|    private void checkCancelled() throws InterruptedException {
    61|    61|        if (cancelled) {
    62|    62|            throw new InterruptedException("Processing cancelled by user");
    63|    63|        }
    64|    64|    }
    65|    65|
    66|    66|    /**
    67|    67|     * Multi-page mode: all images merged into one PDF/OFD.
    68|    68|     */
    69|    69|    public void processMultiPage(List<File> inputFiles, File outputDir, String format,
    70|    70|                                  String language, String ocrEngine, ProgressCallback callback) {
    71|    71|        List<String> outputFiles = new ArrayList<>();
    72|    72|
    73|    73|        try {
    74|    74|            log.info("Processing {} images into multi-page document...", inputFiles.size());
    75|    75|
    76|    76|            // Store all page data
    77|    77|            List<BufferedImage> images = new ArrayList<>();
    78|    78|            List<List<TextBlock>> allTextBlocks = new ArrayList<>();
    79|    79|
    80|    80|            // Process each image
    81|    81|            for (int i = 0; i < inputFiles.size(); i++) {
    82|    82|                checkCancelled();
    83|    83|
    84|    84|                File inputFile = inputFiles.get(i);
    85|    85|                String msg = "[" + (i + 1) + "/" + inputFiles.size() + "] " + inputFile.getName();
    86|    86|                log.info(msg);
    87|    87|                if (callback != null) callback.onProgress(i + 1, inputFiles.size(), msg);
    88|    88|
    89|    89|                try {
    90|    90|                    // Load image
    91|    91|                    BufferedImage image = ImageIO.read(inputFile);
    92|    92|                    if (image == null) {
    93|    93|                        log.error("  ERROR: Cannot read image");
    94|    94|                        continue;
    95|    95|                    }
    96|    96|
    97|    97|                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());
    98|    98|
    99|    99|                    // OCR recognition
   100|   100|                    log.info("  Running OCR...");
   101|   101|                    List<TextBlock> textBlocks;
   102|   102|                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
   103|   103|                        if (tesseractService == null) {
   104|   104|                            tesseractService = new TesseractOcrService(
   105|   105|                                config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
   106|   106|                            log.info("  OCR Engine: Tesseract ({})", TesseractLanguageHelper.getTesseractLabel(language));
   107|   107|                        }
   108|   108|                        textBlocks = tesseractService.recognize(image);
   109|   109|                    } else {
   110|   110|                        textBlocks = ocrService.recognize(image, language);
   111|   111|                    }
   112|   112|
   113|   113|                    // Text conversion
   114|   114|                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
   115|   115|                        convertTextBlocks(textBlocks, config.getTextConvert());
   116|   116|                        log.info("  OK: Text converted ({})", config.getTextConvert());
   117|   117|                    }
   118|   118|
   119|   119|                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());
   120|   120|
   121|   121|                    // Save data
   122|   122|                    images.add(image);
   123|   123|                    allTextBlocks.add(textBlocks);
   124|   124|
   125|   125|                } catch (Exception e) {
   126|   126|                    log.error("  ERROR: {}", e.getMessage());
   127|   127|                }
   128|   128|            }
   129|   129|
   130|   130|            if (images.isEmpty()) {
   131|   131|                String errorMsg = "No valid images processed";
   132|   132|                log.error("ERROR: {}", errorMsg);
   133|   133|                if (callback != null) callback.onError(errorMsg);
   134|   134|                return;
   135|   135|            }
   136|   136|
   137|   137|            log.info("Generating multi-page output...");
   138|   138|
   139|   139|            // Generate output filename
   140|   140|            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
   141|   141|            String outputFilename = "multipage_" + timestamp;
   142|   142|
   143|   143|            // Generate multi-page PDF
   144|   144|            if (format.contains("pdf") || format.contains("all")) {
   145|   145|                checkCancelled();
   146|   146|                File pdfFile = new File(outputDir, outputFilename + ".pdf");
   147|   147|                pdfService.generateMultiPagePdf(images, allTextBlocks, pdfFile);
   148|   148|                log.info("  OK: Multi-page PDF -> {}", pdfFile.getName());
   149|   149|                outputFiles.add(pdfFile.getAbsolutePath());
   150|   150|            }
   151|   151|
   152|   152|            // Generate TXT (all pages text)
   153|   153|            if (format.contains("txt") || format.contains("all")) {
   154|   154|                checkCancelled();
   155|   155|                File txtFile = new File(outputDir, outputFilename + ".txt");
   156|   156|                textService.generateMultiPageTxt(allTextBlocks, txtFile);
   157|   157|                log.info("  OK: TXT -> {}", txtFile.getName());
   158|   158|                outputFiles.add(txtFile.getAbsolutePath());
   159|   159|            }
   160|   160|
   161|   161|            // Generate multi-page OFD
   162|   162|            if (format.contains("ofd") || format.contains("all")) {
   163|   163|                checkCancelled();
   164|   164|                File ofdFile = new File(outputDir, outputFilename + ".ofd");
   165|   165|                ofdService.generateMultiPageOfd(images, allTextBlocks, ofdFile);
   166|   166|                log.info("  OK: Multi-page OFD -> {}", ofdFile.getName());
   167|   167|                outputFiles.add(ofdFile.getAbsolutePath());
   168|   168|            }
   169|   169|
   170|   170|            log.info("Total pages: {}", images.size());
   171|   171|
   172|   172|            if (callback != null) callback.onComplete(outputFiles);
   173|   173|
   174|   174|        } catch (InterruptedException e) {
   175|   175|            log.info("Processing cancelled");
   176|   176|            if (callback != null) callback.onError("Cancelled by user");
   177|   177|        } catch (Exception e) {
   178|   178|            String errorMsg = "Error in multi-page processing: " + e.getMessage();
   179|   179|            log.error("ERROR: {}", errorMsg, e);
   180|   180|            if (callback != null) callback.onError(errorMsg);
   181|   181|        }
   182|   182|    }
   183|   183|
   184|   184|    /**
   185|   185|     * Per-page mode: each image generates one PDF/OFD.
   186|   186|     */
   187|   187|    public void processPerPage(List<File> inputFiles, File outputDir, String format,
   188|   188|                                String language, String ocrEngine, ProgressCallback callback) {
   189|   189|        List<String> outputFiles = new ArrayList<>();
   190|   190|        int processed = 0;
   191|   191|        int failed = 0;
   192|   192|
   193|   193|        try {
   194|   194|            for (int i = 0; i < inputFiles.size(); i++) {
   195|   195|                checkCancelled();
   196|   196|
   197|   197|                File inputFile = inputFiles.get(i);
   198|   198|                processed++;
   199|   199|                String msg = "[" + processed + "/" + inputFiles.size() + "] Processing: " + inputFile.getName();
   200|   200|                log.info(msg);
   201|   201|                if (callback != null) callback.onProgress(processed, inputFiles.size(), msg);
   202|   202|
   203|   203|                try {
   204|   204|                    // Load image
   205|   205|                    BufferedImage image = ImageIO.read(inputFile);
   206|   206|                    if (image == null) {
   207|   207|                        log.error("  ERROR: Cannot read image");
   208|   208|                        failed++;
   209|   209|                        continue;
   210|   210|                    }
   211|   211|
   212|   212|                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());
   213|   213|
   214|   214|                    // OCR recognition
   215|   215|                    log.info("  Running OCR...");
   216|   216|                    List<TextBlock> textBlocks;
   217|   217|                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
   218|   218|                        if (tesseractService == null) {
   219|   219|                            tesseractService = new TesseractOcrService(
   220|   220|                                config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
   221|   221|                            log.info("  OCR Engine: Tesseract ({})", TesseractLanguageHelper.getTesseractLabel(language));
   222|   222|                        }
   223|   223|                        textBlocks = tesseractService.recognize(image);
   224|   224|                    } else {
   225|   225|                        textBlocks = ocrService.recognize(image, language);
   226|   226|                    }
   227|   227|
   228|   228|                    // Text conversion
   229|   229|                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
   230|   230|                        convertTextBlocks(textBlocks, config.getTextConvert());
   231|   231|                        log.info("  OK: Text converted ({})", config.getTextConvert());
   232|   232|                    }
   233|   233|
   234|   234|                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());
   235|   235|
   236|   236|                    // Generate output
   237|   237|                    String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
   238|   238|                    String baseName = getBaseName(inputFile.getName());
   239|   239|                    String outputFilename = baseName + "_" + timestamp;
   240|   240|
   241|   241|                    // Export files
   242|   242|                    if (format.contains("pdf") || format.contains("all")) {
   243|   243|                        checkCancelled();
   244|   244|                        File pdfFile = new File(outputDir, outputFilename + ".pdf");
   245|   245|                        pdfService.generatePdf(image, textBlocks, pdfFile);
   246|   246|                        log.info("  OK: PDF -> {}", pdfFile.getName());
   247|   247|                        outputFiles.add(pdfFile.getAbsolutePath());
   248|   248|                    }
   249|   249|
   250|   250|                    if (format.contains("txt") || format.contains("all")) {
   251|   251|                        checkCancelled();
   252|   252|                        File txtFile = new File(outputDir, outputFilename + ".txt");
   253|   253|                        textService.generateTxt(textBlocks, txtFile);
   254|   254|                        log.info("  OK: TXT -> {}", txtFile.getName());
   255|   255|                        outputFiles.add(txtFile.getAbsolutePath());
   256|   256|                    }
   257|   257|
   258|   258|                    if (format.contains("ofd") || format.contains("all")) {
   259|   259|                        checkCancelled();
   260|   260|                        File ofdFile = new File(outputDir, outputFilename + ".ofd");
   261|   261|                        ofdService.generateOfd(image, textBlocks, ofdFile);
   262|   262|                        log.info("  OK: OFD -> {}", ofdFile.getName());
   263|   263|                        outputFiles.add(ofdFile.getAbsolutePath());
   264|   264|                    }
   265|   265|
   266|   266|                } catch (Exception e) {
   267|   267|                    log.error("  ERROR: {}", e.getMessage(), e);
   268|   268|                    failed++;
   269|   269|                }
   270|   270|            }
   271|   271|
   272|   272|            log.info("");
   273|   273|            log.info("========================================");
   274|   274|            log.info("  Summary");
   275|   275|            log.info("========================================");
   276|   276|            log.info("Processed: {}", processed);
   277|   277|            log.info("Failed:    {}", failed);
   278|   278|
   279|   279|            if (failed == 0) {
   280|   280|                log.info("SUCCESS: All files processed");
   281|   281|            } else {
   282|   282|                log.warn("WARNING: Some files failed");
   283|   283|            }
   284|   284|
   285|   285|            if (callback != null) callback.onComplete(outputFiles);
   286|   286|
   287|   287|        } catch (InterruptedException e) {
   288|   288|            log.info("Processing cancelled");
   289|   289|            if (callback != null) callback.onError("Cancelled by user");
   290|   290|        } catch (Exception e) {
   291|   291|            String errorMsg = "Error in per-page processing: " + e.getMessage();
   292|   292|            log.error("ERROR: {}", errorMsg, e);
   293|   293|            if (callback != null) callback.onError(errorMsg);
   294|   294|        }
   295|   295|    }
   296|   296|
   297|   297|    /**
   298|   298|     * PDF to searchable mode.
   299|   299|     * Renders input PDF files to images, OCRs them, and regenerates searchable PDF/OFD.
   300|   300|     */
   301|   301|    public void processPdfToSearchable(List<File> pdfFiles, File outputDir, String format,
   302|   302|                                        String language, String ocrEngine, float dpi,
   303|   303|                                        ProgressCallback callback) {
   304|   304|        List<String> outputFiles = new ArrayList<>();
   305|   305|
   306|   306|        try {
   307|   307|            PdfToImagesService pdfToImages = new PdfToImagesService();
   308|   308|
   309|   309|            for (int f = 0; f < pdfFiles.size(); f++) {
   310|   310|                checkCancelled();
   311|   311|
   312|   312|                File pdfFile = pdfFiles.get(f);
   313|   313|                String msg = "[" + (f + 1) + "/" + pdfFiles.size() + "] " + pdfFile.getName();
   314|   314|                log.info(msg);
   315|   315|                if (callback != null) callback.onProgress(f + 1, pdfFiles.size(), msg);
   316|   316|
   317|   317|                // Render each PDF page to image
   318|   318|                List<BufferedImage> pages = pdfToImages.renderPages(pdfFile, dpi);
   319|   319|
   320|   320|                // OCR each page
   321|   321|                List<List<TextBlock>> allTextBlocks = new ArrayList<>();
   322|   322|                for (int i = 0; i < pages.size(); i++) {
   323|   323|                    checkCancelled();
   324|   324|
   325|   325|                    log.info("  [{}/{}] OCR...", i + 1, pages.size());
   326|   326|                    List<TextBlock> textBlocks;
   327|   327|                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
   328|   328|                        if (tesseractService == null) {
   329|   329|                            tesseractService = new TesseractOcrService(
   330|   330|                                    config.getTesseractDataPath(),
   331|   331|                                    TesseractLanguageHelper.getTesseractLanguage(language));
   332|   332|                        }
   333|   333|                        textBlocks = tesseractService.recognize(pages.get(i));
   334|   334|                        log.info("  OCR Engine: Tesseract ({})", TesseractLanguageHelper.getTesseractLabel(language));
   335|   335|                    } else {
   336|   336|                        textBlocks = ocrService.recognize(pages.get(i), language);
   337|   337|                        log.info("  OCR Engine: RapidOCR");
   338|   338|                    }
   339|   339|
   340|   340|                    // Text conversion
   341|   341|                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
   342|   342|                        convertTextBlocks(textBlocks, config.getTextConvert());
   343|   343|                    }
   344|   344|
   345|   345|                    allTextBlocks.add(textBlocks);
   346|   346|                    log.info("  OK: {} blocks", textBlocks.size());
   347|   347|                }
   348|   348|
   349|   349|                // Generate output
   350|   350|                String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
   351|   351|                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
   352|   352|
   353|   353|                if (format.contains("pdf") || format.contains("all")) {
   354|   354|                    checkCancelled();
   355|   355|                    String outName = baseName + "_searchable_" + timestamp + ".pdf";
   356|   356|                    File outFile = new File(outputDir, outName);
   357|   357|                    pdfService.generateMultiPagePdf(pages, allTextBlocks, outFile);
   358|   358|                    log.info("  OK: PDF -> {}", outName);
   359|   359|                    outputFiles.add(outFile.getAbsolutePath());
   360|   360|                }
   361|   361|                if (format.contains("ofd") || format.contains("all")) {
   362|   362|                    checkCancelled();
   363|   363|                    String outName = baseName + "_searchable_" + timestamp + ".ofd";
   364|   364|                    File outFile = new File(outputDir, outName);
   365|   365|                    ofdService.generateMultiPageOfd(pages, allTextBlocks, outFile);
   366|   366|                    log.info("  OK: OFD -> {}", outName);
   367|   367|                    outputFiles.add(outFile.getAbsolutePath());
   368|   368|                }
   369|   369|                if (format.contains("txt") || format.contains("all")) {
   370|   370|                    checkCancelled();
   371|   371|                    String outName = baseName + "_searchable_" + timestamp + ".txt";
   372|   372|                    File outFile = new File(outputDir, outName);
   373|   373|                    textService.generateMultiPageTxt(allTextBlocks, outFile);
   374|   374|                    log.info("  OK: TXT -> {}", outName);
   375|   375|                    outputFiles.add(outFile.getAbsolutePath());
   376|   376|                }
   377|   377|
   378|   378|            }
   379|   379|
   380|   380|            if (callback != null) callback.onComplete(outputFiles);
   381|   381|
   382|   382|        } catch (InterruptedException e) {
   383|   383|            log.info("Processing cancelled");
   384|   384|            if (callback != null) callback.onError("Cancelled by user");
   385|   385|        } catch (Exception e) {
   386|   386|            String errorMsg = "Error in PDF to searchable processing: " + e.getMessage();
   387|   387|            log.error("ERROR: {}", errorMsg, e);
   388|   388|            if (callback != null) callback.onError(errorMsg);
   389|   389|        }
   390|   390|    }
   391|   391|
   392|   392|    /**
   393|   393|     * Parse format string into normalized format.
   394|   394|     * @param format comma-separated format string (e.g., "pdf", "pdf,ofd", "all")
   395|   395|     * @return normalized format string
   396|   396|     */
   397|   397|    public String getOutputFormats(String format) {
   398|   398|        if (format == null || format.isEmpty()) {
   399|   399|            return "pdf";
   400|   400|        }
   401|   401|        return format.toLowerCase().trim();
   402|   402|    }
   403|   403|
   404|   404|    /**
   405|   405|     * Text conversion (simplified/traditional Chinese).
   406|   406|     * @param textBlocks OCR recognized text blocks
   407|   407|     * @param mode "s2t" (simplified to traditional) or "t2s" (traditional to simplified)
   408|   408|     */
   409|   409|    private void convertTextBlocks(List<TextBlock> textBlocks, String mode) {
   410|   410|        for (TextBlock block : textBlocks) {
   411|   411|            String text = block.getText();
   412|   412|            if (text == null || text.isEmpty()) continue;
   413|   413|
   414|   414|            if ("s2t".equalsIgnoreCase(mode)) {
   415|   415|                text = ZhConverterUtil.toTraditional(text);
   416|   416|            } else if ("t2s".equalsIgnoreCase(mode)) {
   417|   417|                text = ZhConverterUtil.toSimple(text);
   418|   418|            }
   419|   419|
   420|   420|            block.getText() = text;
   421|   421|        }
   422|   422|    }
   423|   423|
   424|   424|    private String getBaseName(String filename) {
   425|   425|        int dotIndex = filename.lastIndexOf('.');
   426|   426|        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
   427|   427|    }
   428|   428|}
   429|   429|