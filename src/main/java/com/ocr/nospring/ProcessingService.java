package com.ocr.nospring;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Processing service with progress callback support.
 * Extracts processing logic from Main.java into reusable methods.
 */
public class ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Callback interface for progress updates.
     */
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
        void onComplete(List<String> outputFiles);
        void onError(String message);
    }

    private final Config config;
    private final OcrService ocrService;
    private final PdfService pdfService;
    private final TextService textService;
    private final OfdService ofdService;
    private volatile TesseractOcrService tesseractService;

    private volatile boolean cancelled = false;

    /**
     * Constructor that takes Config and initializes all services.
     */
    public ProcessingService(Config config) {
        this.config = config;
        this.ocrService = new OcrService();
        this.pdfService = new PdfService(config);
        this.textService = new TextService();
        this.ofdService = new OfdService(config);
    }

    /**
     * Cancel the current processing operation.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Check if cancelled and throw InterruptedException if so.
     */
    private void checkCancelled() throws InterruptedException {
        if (cancelled) {
            throw new InterruptedException("Processing cancelled by user");
        }
    }

    /**
     * 執行 OCR 辨識，自動選擇引擎（thread-safe lazy init for Tesseract）
     */
    private synchronized List<TextBlock> runOcr(BufferedImage image, String language, String ocrEngine) throws Exception {
        List<TextBlock> textBlocks;
        if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
            if (tesseractService == null) {
                tesseractService = new TesseractOcrService(
                    config.getTesseractDataPath(),
                    TesseractLanguageHelper.getTesseractLanguage(language));
                log.info("  OCR Engine: Tesseract ({})",
                    TesseractLanguageHelper.getTesseractLabel(language));
            }
            textBlocks = tesseractService.recognize(image);
        } else {
            textBlocks = ocrService.recognize(image, language);
            log.info("  OCR Engine: RapidOCR");
        }
        return textBlocks;
    }

    /**
     * 執行文字簡繁轉換（如果設定中有指定）
     */
    private void applyTextConversion(List<TextBlock> textBlocks) {
        if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
            convertTextBlocks(textBlocks, config.getTextConvert());
            log.info("  OK: Text converted ({})", config.getTextConvert());
        }
    }

    /**
     * 產生時間戳字串
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    /**
     * Multi-page mode: all images merged into one PDF/OFD.
     */
    public void processMultiPage(List<File> inputFiles, File outputDir, String format,
                                  String language, String ocrEngine, ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();

        try {
            log.info("Processing {} images into multi-page document...", inputFiles.size());

            List<File> validFiles = new ArrayList<>();
            List<List<TextBlock>> allTextBlocks = new ArrayList<>();

            for (int i = 0; i < inputFiles.size(); i++) {
                checkCancelled();

                File inputFile = inputFiles.get(i);
                String msg = "[" + (i + 1) + "/" + inputFiles.size() + "] " + inputFile.getName();
                log.info(msg);
                if (callback != null) callback.onProgress(i + 1, inputFiles.size(), msg);

                try {
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        log.error("  ERROR: Cannot read image");
                        continue;
                    }

                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());

                    log.info("  Running OCR...");
                    List<TextBlock> textBlocks = runOcr(image, language, ocrEngine);

                    applyTextConversion(textBlocks);

                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                    image.flush();

                    validFiles.add(inputFile);
                    allTextBlocks.add(textBlocks);

                } catch (Exception e) {
                    log.error("  ERROR: {}", e.getMessage());
                }
            }

            if (validFiles.isEmpty()) {
                String errorMsg = "No valid images processed";
                log.error("ERROR: {}", errorMsg);
                if (callback != null) callback.onError(errorMsg);
                return;
            }

            log.info("Generating multi-page output...");

            String outputFilename = "multipage_" + getTimestamp();

            if (format.contains("pdf") || format.contains("all")) {
                checkCancelled();
                File pdfFile = new File(outputDir, outputFilename + ".pdf");
                pdfService.generateMultiPagePdfFromFiles(validFiles, allTextBlocks, pdfFile);
                log.info("  OK: Multi-page PDF -> {}", pdfFile.getName());
                outputFiles.add(pdfFile.getAbsolutePath());
            }

            if (format.contains("txt") || format.contains("all")) {
                checkCancelled();
                File txtFile = new File(outputDir, outputFilename + ".txt");
                textService.generateMultiPageTxt(allTextBlocks, txtFile);
                log.info("  OK: TXT -> {}", txtFile.getName());
                outputFiles.add(txtFile.getAbsolutePath());
            }

            if (format.contains("ofd") || format.contains("all")) {
                checkCancelled();
                File ofdFile = new File(outputDir, outputFilename + ".ofd");
                ofdService.generateMultiPageOfdFromFiles(validFiles, allTextBlocks, ofdFile);
                log.info("  OK: Multi-page OFD -> {}", ofdFile.getName());
                outputFiles.add(ofdFile.getAbsolutePath());
            }

            log.info("Total pages: {}", validFiles.size());

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            log.info("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in multi-page processing: " + e.getMessage();
            log.error("ERROR: {}", errorMsg, e);
            if (callback != null) callback.onError(errorMsg);
        }
    }

    /**
     * Per-page mode: each image generates one PDF/OFD.
     */
    public void processPerPage(List<File> inputFiles, File outputDir, String format,
                                String language, String ocrEngine, ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();
        int processed = 0;
        int failed = 0;

        try {
            for (int i = 0; i < inputFiles.size(); i++) {
                checkCancelled();

                File inputFile = inputFiles.get(i);
                processed++;
                String msg = "[" + processed + "/" + inputFiles.size() + "] Processing: " + inputFile.getName();
                log.info(msg);
                if (callback != null) callback.onProgress(processed, inputFiles.size(), msg);

                try {
                    // Load image
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        log.error("  ERROR: Cannot read image");
                        failed++;
                        continue;
                    }

                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());

                    // OCR recognition
                    log.info("  Running OCR...");
                    List<TextBlock> textBlocks = runOcr(image, language, ocrEngine);

                    // Text conversion
                    applyTextConversion(textBlocks);

                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                    // Generate output
                    String baseName = getBaseName(inputFile.getName());
                    String outputFilename = baseName + "_" + getTimestamp();

                    // Export files
                    if (format.contains("pdf") || format.contains("all")) {
                        checkCancelled();
                        File pdfFile = new File(outputDir, outputFilename + ".pdf");
                        pdfService.generatePdf(image, textBlocks, pdfFile);
                        log.info("  OK: PDF -> {}", pdfFile.getName());
                        outputFiles.add(pdfFile.getAbsolutePath());
                    }

                    if (format.contains("txt") || format.contains("all")) {
                        checkCancelled();
                        File txtFile = new File(outputDir, outputFilename + ".txt");
                        textService.generateTxt(textBlocks, txtFile);
                        log.info("  OK: TXT -> {}", txtFile.getName());
                        outputFiles.add(txtFile.getAbsolutePath());
                    }

                    if (format.contains("ofd") || format.contains("all")) {
                        checkCancelled();
                        File ofdFile = new File(outputDir, outputFilename + ".ofd");
                        ofdService.generateOfd(image, textBlocks, ofdFile);
                        log.info("  OK: OFD -> {}", ofdFile.getName());
                        outputFiles.add(ofdFile.getAbsolutePath());
                    }

                } catch (Exception e) {
                    log.error("  ERROR: {}", e.getMessage(), e);
                    failed++;
                }
            }

            log.info("");
            log.info("========================================");
            log.info("  Summary");
            log.info("========================================");
            log.info("Processed: {}", processed);
            log.info("Failed:    {}", failed);

            if (failed == 0) {
                log.info("SUCCESS: All files processed");
            } else {
                log.warn("WARNING: Some files failed");
            }

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            log.info("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in per-page processing: " + e.getMessage();
            log.error("ERROR: {}", errorMsg, e);
            if (callback != null) callback.onError(errorMsg);
        }
    }

    /**
     * PDF to searchable mode.
     * Renders input PDF files to images, OCRs them, and regenerates searchable PDF/OFD.
     */
    public void processPdfToSearchable(List<File> pdfFiles, File outputDir, String format,
                                        String language, String ocrEngine, float dpi,
                                        ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();

        try {
            PdfToImagesService pdfToImages = new PdfToImagesService();

            for (int f = 0; f < pdfFiles.size(); f++) {
                checkCancelled();

                File pdfFile = pdfFiles.get(f);
                String msg = "[" + (f + 1) + "/" + pdfFiles.size() + "] " + pdfFile.getName();
                log.info(msg);
                if (callback != null) callback.onProgress(f + 1, pdfFiles.size(), msg);

                // Render each PDF page to image
                List<BufferedImage> pages = pdfToImages.renderPages(pdfFile, dpi);

                // OCR each page
                List<List<TextBlock>> allTextBlocks = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    checkCancelled();

                    log.info("  [{}/{}] OCR...", i + 1, pages.size());
                    List<TextBlock> textBlocks = runOcr(pages.get(i), language, ocrEngine);

                    // Text conversion
                    applyTextConversion(textBlocks);

                    allTextBlocks.add(textBlocks);
                    log.info("  OK: {} blocks", textBlocks.size());
                }

                // Generate output
                String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
                String timestamp = getTimestamp();

                if (format.contains("pdf") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".pdf";
                    File outFile = new File(outputDir, outName);
                    pdfService.generateMultiPagePdf(pages, allTextBlocks, outFile);
                    log.info("  OK: PDF -> {}", outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }
                if (format.contains("ofd") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".ofd";
                    File outFile = new File(outputDir, outName);
                    ofdService.generateMultiPageOfd(pages, allTextBlocks, outFile);
                    log.info("  OK: OFD -> {}", outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }
                if (format.contains("txt") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".txt";
                    File outFile = new File(outputDir, outName);
                    textService.generateMultiPageTxt(allTextBlocks, outFile);
                    log.info("  OK: TXT -> {}", outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }

            }

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            log.info("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in PDF to searchable processing: " + e.getMessage();
            log.error("ERROR: {}", errorMsg, e);
            if (callback != null) callback.onError(errorMsg);
        }
    }

    /**
     * Parse format string into normalized format.
     * @param format comma-separated format string (e.g., "pdf", "pdf,ofd", "all")
     * @return normalized format string
     */
    public String getOutputFormats(String format) {
        if (format == null || format.isEmpty()) {
            return "pdf";
        }
        return format.toLowerCase().trim();
    }

    /**
     * Text conversion (simplified/traditional Chinese).
     * @param textBlocks OCR recognized text blocks
     * @param mode "s2t" (simplified to traditional) or "t2s" (traditional to simplified)
     */
    private void convertTextBlocks(List<TextBlock> textBlocks, String mode) {
        for (TextBlock block : textBlocks) {
            String text = block.getText();
            if (text == null || text.isEmpty()) continue;

            if ("s2t".equalsIgnoreCase(mode)) {
                text = ZhConverterUtil.toTraditional(text);
            } else if ("t2s".equalsIgnoreCase(mode)) {
                text = ZhConverterUtil.toSimple(text);
            }

            block.setText(text);
        }
    }

    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
