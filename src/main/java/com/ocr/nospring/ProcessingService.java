package com.ocr.nospring;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Processing service with progress callback support.
 * Extracts processing logic from Main.java into reusable methods.
 */
public class ProcessingService {

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
    private TesseractOcrService tesseractService;

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
     * Multi-page mode: all images merged into one PDF/OFD.
     */
    public void processMultiPage(List<File> inputFiles, File outputDir, String format,
                                  String language, String ocrEngine, ProgressCallback callback) {
        List<String> outputFiles = new ArrayList<>();

        try {
            System.out.println("Processing " + inputFiles.size() + " images into multi-page document...");
            System.out.println();

            // Store all page data
            List<BufferedImage> images = new ArrayList<>();
            List<List<OcrService.TextBlock>> allTextBlocks = new ArrayList<>();

            // Process each image
            for (int i = 0; i < inputFiles.size(); i++) {
                checkCancelled();

                File inputFile = inputFiles.get(i);
                String msg = "[" + (i + 1) + "/" + inputFiles.size() + "] " + inputFile.getName();
                System.out.println(msg);
                if (callback != null) callback.onProgress(i + 1, inputFiles.size(), msg);

                try {
                    // Load image
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        System.err.println("  ERROR: Cannot read image");
                        continue;
                    }

                    System.out.println("  Image size: " + image.getWidth() + "x" + image.getHeight());

                    // OCR recognition
                    System.out.println("  Running OCR...");
                    List<OcrService.TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        if (tesseractService == null) {
                            tesseractService = new TesseractOcrService(
                                config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
                            System.out.println("  OCR Engine: Tesseract (" + TesseractLanguageHelper.getTesseractLabel(language) + ")");
                        }
                        textBlocks = tesseractService.recognize(image);
                    } else {
                        textBlocks = ocrService.recognize(image, language);
                    }

                    // Text conversion
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                        System.out.println("  OK: Text converted (" + config.getTextConvert() + ")");
                    }

                    System.out.println("  OK: OCR completed (" + textBlocks.size() + " blocks)");

                    // Save data
                    images.add(image);
                    allTextBlocks.add(textBlocks);

                } catch (Exception e) {
                    System.err.println("  ERROR: " + e.getMessage());
                }
            }

            if (images.isEmpty()) {
                String errorMsg = "No valid images processed";
                System.err.println("ERROR: " + errorMsg);
                if (callback != null) callback.onError(errorMsg);
                return;
            }

            System.out.println();
            System.out.println("Generating multi-page output...");

            // Generate output filename
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputFilename = "multipage_" + timestamp;

            // Generate multi-page PDF
            if (format.contains("pdf") || format.contains("all")) {
                checkCancelled();
                File pdfFile = new File(outputDir, outputFilename + ".pdf");
                pdfService.generateMultiPagePdf(images, allTextBlocks, pdfFile);
                System.out.println("  OK: Multi-page PDF -> " + pdfFile.getName());
                outputFiles.add(pdfFile.getAbsolutePath());
            }

            // Generate TXT (all pages text)
            if (format.contains("txt") || format.contains("all")) {
                checkCancelled();
                File txtFile = new File(outputDir, outputFilename + ".txt");
                textService.generateMultiPageTxt(allTextBlocks, txtFile);
                System.out.println("  OK: TXT -> " + txtFile.getName());
                outputFiles.add(txtFile.getAbsolutePath());
            }

            // Generate multi-page OFD
            if (format.contains("ofd") || format.contains("all")) {
                checkCancelled();
                File ofdFile = new File(outputDir, outputFilename + ".ofd");
                ofdService.generateMultiPageOfd(images, allTextBlocks, ofdFile);
                System.out.println("  OK: Multi-page OFD -> " + ofdFile.getName());
                outputFiles.add(ofdFile.getAbsolutePath());
            }

            System.out.println();
            System.out.println("Total pages: " + images.size());

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            System.out.println("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in multi-page processing: " + e.getMessage();
            System.err.println("ERROR: " + errorMsg);
            e.printStackTrace();
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
                System.out.println(msg);
                if (callback != null) callback.onProgress(processed, inputFiles.size(), msg);

                try {
                    // Load image
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        System.err.println("  ERROR: Cannot read image");
                        failed++;
                        continue;
                    }

                    System.out.println("  Image size: " + image.getWidth() + "x" + image.getHeight());

                    // OCR recognition
                    System.out.println("  Running OCR...");
                    List<OcrService.TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        if (tesseractService == null) {
                            tesseractService = new TesseractOcrService(
                                config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
                            System.out.println("  OCR Engine: Tesseract (" + TesseractLanguageHelper.getTesseractLabel(language) + ")");
                        }
                        textBlocks = tesseractService.recognize(image);
                    } else {
                        textBlocks = ocrService.recognize(image, language);
                    }

                    // Text conversion
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                        System.out.println("  OK: Text converted (" + config.getTextConvert() + ")");
                    }

                    System.out.println("  OK: OCR completed (" + textBlocks.size() + " blocks)");

                    // Generate output
                    String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String baseName = getBaseName(inputFile.getName());
                    String outputFilename = baseName + "_" + timestamp;

                    // Export files
                    if (format.contains("pdf") || format.contains("all")) {
                        checkCancelled();
                        File pdfFile = new File(outputDir, outputFilename + ".pdf");
                        pdfService.generatePdf(image, textBlocks, pdfFile);
                        System.out.println("  OK: PDF -> " + pdfFile.getName());
                        outputFiles.add(pdfFile.getAbsolutePath());
                    }

                    if (format.contains("txt") || format.contains("all")) {
                        checkCancelled();
                        File txtFile = new File(outputDir, outputFilename + ".txt");
                        textService.generateTxt(textBlocks, txtFile);
                        System.out.println("  OK: TXT -> " + txtFile.getName());
                        outputFiles.add(txtFile.getAbsolutePath());
                    }

                    if (format.contains("ofd") || format.contains("all")) {
                        checkCancelled();
                        File ofdFile = new File(outputDir, outputFilename + ".ofd");
                        ofdService.generateOfd(image, textBlocks, ofdFile);
                        System.out.println("  OK: OFD -> " + ofdFile.getName());
                        outputFiles.add(ofdFile.getAbsolutePath());
                    }

                    System.out.println();

                } catch (Exception e) {
                    System.err.println("  ERROR: " + e.getMessage());
                    e.printStackTrace();
                    failed++;
                }
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("  Summary");
            System.out.println("========================================");
            System.out.println("Processed: " + processed);
            System.out.println("Failed:    " + failed);
            System.out.println();

            if (failed == 0) {
                System.out.println("SUCCESS: All files processed");
            } else {
                System.out.println("WARNING: Some files failed");
            }

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            System.out.println("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in per-page processing: " + e.getMessage();
            System.err.println("ERROR: " + errorMsg);
            e.printStackTrace();
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
                System.out.println(msg);
                if (callback != null) callback.onProgress(f + 1, pdfFiles.size(), msg);
                System.out.println();

                // Render each PDF page to image
                List<BufferedImage> pages = pdfToImages.renderPages(pdfFile, dpi);

                // OCR each page
                List<List<OcrService.TextBlock>> allTextBlocks = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    checkCancelled();

                    System.out.println("  [" + (i + 1) + "/" + pages.size() + "] OCR...");
                    List<OcrService.TextBlock> textBlocks;
                    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
                        if (tesseractService == null) {
                            tesseractService = new TesseractOcrService(
                                    config.getTesseractDataPath(),
                                    TesseractLanguageHelper.getTesseractLanguage(language));
                        }
                        textBlocks = tesseractService.recognize(pages.get(i));
                        System.out.println("  OCR Engine: Tesseract (" + TesseractLanguageHelper.getTesseractLabel(language) + ")");
                    } else {
                        textBlocks = ocrService.recognize(pages.get(i), language);
                        System.out.println("  OCR Engine: RapidOCR");
                    }

                    // Text conversion
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                    }

                    allTextBlocks.add(textBlocks);
                    System.out.println("  OK: " + textBlocks.size() + " blocks");
                }

                // Generate output
                String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

                if (format.contains("pdf") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".pdf";
                    File outFile = new File(outputDir, outName);
                    pdfService.generateMultiPagePdf(pages, allTextBlocks, outFile);
                    System.out.println("  OK: PDF -> " + outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }
                if (format.contains("ofd") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".ofd";
                    File outFile = new File(outputDir, outName);
                    ofdService.generateMultiPageOfd(pages, allTextBlocks, outFile);
                    System.out.println("  OK: OFD -> " + outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }
                if (format.contains("txt") || format.contains("all")) {
                    checkCancelled();
                    String outName = baseName + "_searchable_" + timestamp + ".txt";
                    File outFile = new File(outputDir, outName);
                    textService.generateMultiPageTxt(allTextBlocks, outFile);
                    System.out.println("  OK: TXT -> " + outName);
                    outputFiles.add(outFile.getAbsolutePath());
                }

                System.out.println();
            }

            if (callback != null) callback.onComplete(outputFiles);

        } catch (InterruptedException e) {
            System.out.println("Processing cancelled");
            if (callback != null) callback.onError("Cancelled by user");
        } catch (Exception e) {
            String errorMsg = "Error in PDF to searchable processing: " + e.getMessage();
            System.err.println("ERROR: " + errorMsg);
            e.printStackTrace();
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
    private void convertTextBlocks(List<OcrService.TextBlock> textBlocks, String mode) {
        for (OcrService.TextBlock block : textBlocks) {
            String text = block.text;
            if (text == null || text.isEmpty()) continue;

            if ("s2t".equalsIgnoreCase(mode)) {
                text = ZhConverterUtil.toTraditional(text);
            } else if ("t2s".equalsIgnoreCase(mode)) {
                text = ZhConverterUtil.toSimple(text);
            }

            block.text = text;
        }
    }

    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
