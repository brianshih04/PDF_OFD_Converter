package com.ocr.nospring;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Processing service with progress callback support.
 * Extracts processing logic from Main.java into reusable methods.
 */
public class ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

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
     * Constructor with dependency injection for all services.
     */
    public ProcessingService(Config config, OcrService ocrService, PdfService pdfService,
                             OfdService ofdService, TextService textService) {
        this.config = config;
        this.ocrService = ocrService;
        this.pdfService = pdfService;
        this.textService = textService;
        this.ofdService = ofdService;
    }

    /**
     * Cancel the current processing operation.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Thread-safe lazy initialization of TesseractOcrService.
     */
    private synchronized TesseractOcrService getOrCreateTesseractService(String dataPath, String language) throws Exception {
        if (tesseractService == null) {
            tesseractService = new TesseractOcrService(dataPath, language);
            log.info("  OCR Engine: Tesseract ({})", TesseractLanguageHelper.getTesseractLabel(language));
        }
        return tesseractService;
    }

    /**
     * Check if cancelled and throw InterruptedException if so.
     */
    private void checkCancelled() throws InterruptedException {
        if (cancelled) {
            throw new InterruptedException("Processing cancelled by user");
        }
    }

    private static final long LOW_MEMORY_THRESHOLD_BYTES = 50 * 1024 * 1024; // 50 MB

    private static final java.time.format.DateTimeFormatter TIMESTAMP_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Multi-page mode: all images merged into one PDF/OFD.
     * Uses streaming approach — only one image in memory at a time.
     */
    public void processMultiPage(List<File> inputFiles, File outputDir, String format,
                                  String language, String ocrEngine, ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();

        // Streaming state — one image processed at a time
        PDDocument pdfDoc = null;
        boolean pdfOpen = false;
        boolean txtOpen = false;
        boolean ofdOpen = false;
        int totalPages = 0;

        try {
            log.info("Processing {} images into multi-page document...", inputFiles.size());

            // Generate output filename
            String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String outputFilename = "multipage_" + timestamp;

            // Pre-create output files so we can stream into them
            File pdfFile = (format.contains("pdf") || format.contains("all"))
                ? new File(outputDir, outputFilename + ".pdf") : null;
            File txtFile = (format.contains("txt") || format.contains("all"))
                ? new File(outputDir, outputFilename + ".txt") : null;
            File ofdFile = (format.contains("ofd") || format.contains("all"))
                ? new File(outputDir, outputFilename + ".ofd") : null;

            // Process each image: load → OCR → add to outputs → release
            for (int i = 0; i < inputFiles.size(); i++) {
                checkCancelled();
                checkMemoryPressure();

                File inputFile = inputFiles.get(i);
                String msg = "[" + (i + 1) + "/" + inputFiles.size() + "] " + inputFile.getName();
                log.info(msg);
                if (callback != null) callback.onProgress(i + 1, inputFiles.size(), msg);

                try {
                    // Load image
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        log.error("  ERROR: Cannot read image");
                        continue;
                    }

                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());

                    // OCR recognition
                    log.info("  Running OCR...");
                    List<TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        tesseractService = getOrCreateTesseractService(
                            config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
                        textBlocks = tesseractService.recognize(image);
                    } else {
                        textBlocks = ocrService.recognize(image, language);
                    }

                    // Text conversion
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                        log.info("  OK: Text converted ({})", config.getTextConvert());
                    }

                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                    // Open output documents on first valid page
                    if (totalPages == 0) {
                        if (pdfFile != null) {
                            pdfDoc = pdfService.openMultiPage(pdfFile);
                            pdfOpen = true;
                        }
                        if (txtFile != null) {
                            textService.openMultiPage(txtFile);
                            txtOpen = true;
                        }
                        if (ofdFile != null) {
                            ofdService.openMultiPage(ofdFile);
                            ofdOpen = true;
                        }
                    }

                    // Add page to each open output
                    if (pdfOpen) pdfService.addPage(pdfDoc, image, textBlocks);
                    if (txtOpen) textService.addPage(textBlocks, totalPages + 1);
                    if (ofdOpen) ofdService.addPage(image, textBlocks, totalPages + 1);

                    totalPages++;

                    // Release image immediately
                    image.flush();

                } catch (Exception e) {
                    log.error("  ERROR: {}", e.getMessage());
                }
            }

            if (totalPages == 0) {
                String errorMsg = "No valid images processed";
                log.error("ERROR: {}", errorMsg);
                if (callback != null) callback.onError(errorMsg);
                return;
            }

            // Close all output documents
            if (pdfOpen) {
                pdfService.closeMultiPage(pdfDoc);
                log.info("  OK: Multi-page PDF -> {}", pdfFile.getName());
                outputFiles.add(pdfFile.getAbsolutePath());
            }
            if (txtOpen) {
                textService.closeMultiPage();
                log.info("  OK: TXT -> {}", txtFile.getName());
                outputFiles.add(txtFile.getAbsolutePath());
            }
            if (ofdOpen) {
                ofdService.closeMultiPage();
                log.info("  OK: Multi-page OFD -> {}", ofdFile.getName());
                outputFiles.add(ofdFile.getAbsolutePath());
            }

            log.info("Total pages: {}", totalPages);

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            log.info("Processing cancelled");
            // Clean up partial outputs
            try { if (pdfOpen) pdfService.closeMultiPage(pdfDoc); } catch (Exception ignored) {}
            try { if (txtOpen) textService.closeMultiPage(); } catch (Exception ignored) {}
            try { if (ofdOpen) ofdService.closeMultiPage(); } catch (Exception ignored) {}
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in multi-page processing: " + e.getMessage();
            log.error("ERROR: {}", errorMsg, e);
            try { if (pdfOpen) pdfService.closeMultiPage(pdfDoc); } catch (Exception ignored) {}
            try { if (txtOpen) textService.closeMultiPage(); } catch (Exception ignored) {}
            try { if (ofdOpen) ofdService.closeMultiPage(); } catch (Exception ignored) {}
            if (callback != null) callback.onError(errorMsg);
        }
    }

    /**
     * Check if available memory is critically low. If so, request GC and pause briefly.
     */
    private void checkMemoryPressure() {
        Runtime rt = Runtime.getRuntime();
        long freeBytes = rt.freeMemory();
        if (freeBytes < LOW_MEMORY_THRESHOLD_BYTES) {
            log.warn("Low memory detected: {}MB free (threshold: {}MB). Pausing for GC...",
                bytesToMb(freeBytes), bytesToMb(LOW_MEMORY_THRESHOLD_BYTES));
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long freeAfter = rt.freeMemory();
            log.info("Memory after GC: {}MB free", bytesToMb(freeAfter));
        }
    }

    private static long bytesToMb(long bytes) {
        return bytes / (1024 * 1024);
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
                    List<TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        tesseractService = getOrCreateTesseractService(
                            config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
                        textBlocks = tesseractService.recognize(image);
                    } else {
                        textBlocks = ocrService.recognize(image, language);
                    }

                    // Text conversion
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                        log.info("  OK: Text converted ({})", config.getTextConvert());
                    }

                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                    // Generate output
                    String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
                    String baseName = getBaseName(inputFile.getName());
                    String outputFilename = baseName + "_" + timestamp;

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
     * Uses streaming approach — only one page image in memory at a time per PDF.
     */
    public void processPdfToSearchable(List<File> pdfFiles, File outputDir, String format,
                                        String language, String ocrEngine, float dpi,
                                        ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();
        PdfToImagesService pdfToImages = new PdfToImagesService();

        for (int f = 0; f < pdfFiles.size(); f++) {
            // Streaming state for this PDF
            PDDocument pdfDoc = null;
            boolean pdfOpen = false;
            boolean txtOpen = false;
            boolean ofdOpen = false;
            int totalPages = 0;

            try {
                checkCancelled();

                File pdfFile = pdfFiles.get(f);
                String msg = "[" + (f + 1) + "/" + pdfFiles.size() + "] " + pdfFile.getName();
                log.info(msg);
                if (callback != null) callback.onProgress(f + 1, pdfFiles.size(), msg);

                // Pre-create output files
                String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
                String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);

                File outFile = (format.contains("pdf") || format.contains("all"))
                    ? new File(outputDir, baseName + "_searchable_" + timestamp + ".pdf") : null;
                File txtFile = (format.contains("txt") || format.contains("all"))
                    ? new File(outputDir, baseName + "_searchable_" + timestamp + ".txt") : null;
                File ofdFile = (format.contains("ofd") || format.contains("all"))
                    ? new File(outputDir, baseName + "_searchable_" + timestamp + ".ofd") : null;

                // Render all pages (PdfToImagesService returns List — this is per-PDF, not cumulative)
                List<BufferedImage> pages = pdfToImages.renderPages(pdfFile, dpi);

                // Process each page: OCR → add to outputs → release
                for (int i = 0; i < pages.size(); i++) {
                    checkCancelled();
                    BufferedImage pageImage = pages.get(i);

                    log.info("  [{}/{}] OCR...", i + 1, pages.size());
                    List<TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        tesseractService = getOrCreateTesseractService(
                            config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
                        textBlocks = tesseractService.recognize(pageImage);
                        log.info("  OCR Engine: Tesseract ({})", TesseractLanguageHelper.getTesseractLabel(language));
                    } else {
                        textBlocks = ocrService.recognize(pageImage, language);
                        log.info("  OCR Engine: RapidOCR");
                    }

                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                    }

                    log.info("  OK: {} blocks", textBlocks.size());

                    // Open output documents on first page
                    if (totalPages == 0) {
                        if (outFile != null) {
                            pdfDoc = pdfService.openMultiPage(outFile);
                            pdfOpen = true;
                        }
                        if (txtFile != null) {
                            textService.openMultiPage(txtFile);
                            txtOpen = true;
                        }
                        if (ofdFile != null) {
                            ofdService.openMultiPage(ofdFile);
                            ofdOpen = true;
                        }
                    }

                    // Add page to each open output
                    if (pdfOpen) pdfService.addPage(pdfDoc, pageImage, textBlocks);
                    if (txtOpen) textService.addPage(textBlocks, totalPages + 1);
                    if (ofdOpen) ofdService.addPage(pageImage, textBlocks, totalPages + 1);

                    totalPages++;

                    // Release page image immediately
                    pageImage.flush();
                }

                // Clear the pages list references
                pages.clear();

                // Close all output documents
                if (pdfOpen) {
                    pdfService.closeMultiPage(pdfDoc);
                    log.info("  OK: PDF -> {}", outFile.getName());
                    outputFiles.add(outFile.getAbsolutePath());
                }
                if (txtOpen) {
                    textService.closeMultiPage();
                    log.info("  OK: TXT -> {}", txtFile.getName());
                    outputFiles.add(txtFile.getAbsolutePath());
                }
                if (ofdOpen) {
                    ofdService.closeMultiPage();
                    log.info("  OK: OFD -> {}", ofdFile.getName());
                    outputFiles.add(ofdFile.getAbsolutePath());
                }

            } catch (InterruptedException e) {
                log.info("Processing cancelled");
                try { if (pdfOpen) pdfService.closeMultiPage(pdfDoc); } catch (Exception ignored) {}
                try { if (txtOpen) textService.closeMultiPage(); } catch (Exception ignored) {}
                try { if (ofdOpen) ofdService.closeMultiPage(); } catch (Exception ignored) {}
                if (callback != null) callback.onError("Cancelled by user");
                return;
            } catch (Exception e) {
                String errorMsg = "Error processing " + pdfFiles.get(f).getName() + ": " + e.getMessage();
                log.error("ERROR: {}", errorMsg, e);
                try { if (pdfOpen) pdfService.closeMultiPage(pdfDoc); } catch (Exception ignored) {}
                try { if (txtOpen) textService.closeMultiPage(); } catch (Exception ignored) {}
                try { if (ofdOpen) ofdService.closeMultiPage(); } catch (Exception ignored) {}
                if (callback != null) callback.onError(errorMsg);
            }
        }

        if (callback != null) callback.onComplete(outputFiles);
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
