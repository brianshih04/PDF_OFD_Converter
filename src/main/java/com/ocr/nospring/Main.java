package com.ocr.nospring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * 純 Java SE 主程序 - 無 Spring Boot
 *
 * 支持兩種輸出模式：
 * 1. perPage (默認) - 每個圖片生成一個 PDF/OFD
 * 2. multiPage - 所有圖片合併成一個多頁 PDF/OFD
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static String VERSION = "3.0.0 (No Spring Boot)";
    private static String APP_NAME = "JPEG2PDF-OFD (No Spring Boot)";

    static {
        // Load version from properties file
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                VERSION = props.getProperty("app.version", VERSION);
                APP_NAME = props.getProperty("app.name", APP_NAME);
            }
        } catch (Exception e) {
            log.warn("Could not load version.properties, using default version");
        }
    }

    public static void main(String[] args) {
        try {
            System.setProperty("java.awt.headless", "true");

            if (args.length == 0) {
                printUsage();
                System.exit(0);
            }

            String configFile = args[0];

            if (configFile.equals("--help") || configFile.equals("-h")) {
                printUsage();
                System.exit(0);
            }

            if (configFile.equals("--version") || configFile.equals("-v")) {
                log.info("{} v{}", APP_NAME, VERSION);
                System.exit(0);
            }

            // 創建配置
            Config config = new Config();

            // 加載配置文件
            File file = new File(configFile);
            if (!file.exists()) {
                log.error("ERROR: Config file not found: {}", configFile);
                log.error("Please check the path or try one of the following .json files in current directory:");

                // List available .json files
                File currentDir = new File(".");
                File[] jsonFiles = currentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
                if (jsonFiles != null && jsonFiles.length > 0) {
                    for (File jsonFile : jsonFiles) {
                        log.error("  - {}", jsonFile.getName());
                    }
                } else {
                    log.error("  (No .json files found in current directory)");
                }

                log.error("Usage: java -jar jpeg2pdf-ofd-nospring.jar <config.json>");
                log.error("Run with --help for more information.");
                System.exit(1);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> configMap = mapper.readValue(file, Map.class);

            log.info("========================================");
            log.info("  {} v{}", APP_NAME, VERSION);
            log.info("========================================");
            log.info("Config: {}", configFile);
            log.info("OK: Config loaded");

            // Validate configuration
            try {
                config.validate();
                log.info("OK: Config validated");
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Configuration validation failed: {}", e.getMessage());
                System.exit(1);
            }

            log.info("Configuration loaded and validated from: {}", configFile);

            // 讀取字體配置 (NPE-safe cast)
            Object fontRaw = configMap.get("font");
            if (fontRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fontConfig = (Map<String, Object>) fontRaw;
                if (fontConfig.containsKey("path")) {
                    String fontPath = (String) fontConfig.get("path");
                    config.setFontPath(fontPath);
                    log.info("Font: {}", fontPath);
                }
            }

            // 讀取文字層配置 (NPE-safe cast)
            Object textLayerRaw = configMap.get("textLayer");
            if (textLayerRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> textLayerConfig = (Map<String, Object>) textLayerRaw;
                // 顏色設定（支持顏色名稱或 RGB）
                if (textLayerConfig.containsKey("color")) {
                    String color = (String) textLayerConfig.get("color");
                    config.setTextLayerColor(color);
                }
                if (textLayerConfig.containsKey("red")) {
                    config.setTextLayerRed(((Number) textLayerConfig.get("red")).intValue());
                }
                if (textLayerConfig.containsKey("green")) {
                    config.setTextLayerGreen(((Number) textLayerConfig.get("green")).intValue());
                }
                if (textLayerConfig.containsKey("blue")) {
                    config.setTextLayerBlue(((Number) textLayerConfig.get("blue")).intValue());
                }

                // 透明度設定
                if (textLayerConfig.containsKey("opacity")) {
                    config.setTextLayerOpacity(((Number) textLayerConfig.get("opacity")).doubleValue());
                }
            }

            // 讀取簡繁轉換配置
            if (configMap.containsKey("textConvert")) {
                String textConvert = (String) configMap.get("textConvert");
                config.setTextConvert(textConvert);
                log.info("Text Convert: {}", textConvert);
            }

            // 創建 Service 實例
            OcrService ocrService = new OcrService();
            PdfService pdfService = new PdfService(config);
            TextService textService = new TextService();
            OfdService ofdService = new OfdService(config);
            TesseractOcrService tesseractService = null;

            // 獲取輸入配置
            @SuppressWarnings("unchecked")
            Map<String, Object> inputConfig = (Map<String, Object>) configMap.get("input");
            @SuppressWarnings("unchecked")
            Map<String, Object> outputConfig = (Map<String, Object>) configMap.get("output");
            @SuppressWarnings("unchecked")
            Map<String, Object> ocrConfig = (Map<String, Object>) configMap.get("ocr");

            // 檢查是否為 PDF 輸入模式（將 PDF 轉為 searchable）
            String inputType = "image"; // default
            if (inputConfig != null && inputConfig.containsKey("type")) {
                inputType = ((String) inputConfig.get("type")).toLowerCase();
            }

            // 獲取 DPI（PDF 渲染用，預設 300）
            float renderDpi = 300f;
            if (inputConfig != null && inputConfig.containsKey("dpi")) {
                renderDpi = ((Number) inputConfig.get("dpi")).floatValue();
            }

            List<File> inputFiles;

            if ("pdf".equals(inputType)) {
                // PDF 輸入模式：提取輸入的 PDF 檔案
                inputFiles = getInputFiles(inputConfig);
                if (inputFiles.isEmpty()) {
                    log.error("ERROR: No PDF files found.");
                    log.error("Suggestions:");
                    log.error("  - Check that the input folder contains PDF files");
                    log.error("  - Verify the file pattern matches .pdf extension");
                    log.error("  - Ensure the files have read permissions");
                    return;
                }
                log.info("Mode: PDF to Searchable");
                log.info("Found {} PDF file(s)", inputFiles.size());
                // 將在後面逐個渲染處理
            } else {
                // 原有圖片輸入模式
                inputFiles = getInputFiles(inputConfig);
                log.info("Found {} file(s)", inputFiles.size());
            }

            if (inputFiles.isEmpty()) {
                log.error("ERROR: No input files found matching the specified pattern.");
                log.error("Suggestions:");
                log.error("  - Check that the input folder path is correct");
                log.error("  - Verify the file pattern (e.g., *.jpg, *.png, *.pdf)");
                log.error("  - Ensure the files exist in the specified location");
                return;
            }

            // 獲取輸出配置
            String outputFolder = getOutputFolder(outputConfig);
            String format = getOutputFormat(outputConfig);
            String language = getOcrLanguage(ocrConfig);
            boolean multiPage = getMultiPageMode(outputConfig);

            // Set OCR language on config for PdfService to use
            config.setOcrLanguage(language);

            // 讀取 OCR 引擎選擇："auto"(預設), "rapidocr", "tesseract"
            String ocrEngine = "auto";
            if (ocrConfig != null && ocrConfig.containsKey("engine")) {
                ocrEngine = ((String) ocrConfig.get("engine")).toLowerCase();
            }
            log.info("OCR Engine: {}", ocrEngine);

            // Read Tesseract tessdata path (only needed for non-RapidOCR languages)
            if (ocrConfig != null && ocrConfig.containsKey("tesseractDataPath")) {
                config.setTesseractDataPath((String) ocrConfig.get("tesseractDataPath"));
            }

            // 創建輸出目錄
            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            log.info("Output: {}", outputFolder);
            log.info("Mode: {}", multiPage ? "Multi-Page" : "Per-Page");

            if ("pdf".equals(inputType)) {
                // PDF 轉 searchable 模式
                processPdfToSearchable(inputFiles, outputDir, format, language, ocrEngine,
                        renderDpi, config, ocrService, pdfService, textService, ofdService, tesseractService);
            } else if (multiPage) {
                // 多頁模式：所有圖片合併成一個 PDF/OFD
                processMultiPage(inputFiles, outputDir, format, language, ocrEngine, config,
                               ocrService, pdfService, textService, ofdService, tesseractService);
            } else {
                // 單頁模式：每個圖片一個 PDF/OFD
                processPerPage(inputFiles, outputDir, format, language, ocrEngine, config,
                             ocrService, pdfService, textService, ofdService, tesseractService);
            }

            log.info("========================================");
            log.info("  Done!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * 多頁模式：所有圖片合併成一個 PDF/OFD
     */
    private static void processMultiPage(List<File> inputFiles, File outputDir,
                                        String format, String language, String ocrEngine, Config config,
                                        OcrService ocrService, PdfService pdfService,
                                        TextService textService, OfdService ofdService,
                                        TesseractOcrService tesseractService) {
        try {
            log.info("Processing {} images into multi-page document...", inputFiles.size());

            // 存儲所有頁面的數據
            List<BufferedImage> images = new ArrayList<>();
            List<List<OcrService.TextBlock>> allTextBlocks = new ArrayList<>();

            // 處理每個圖片
            for (int i = 0; i < inputFiles.size(); i++) {
                File inputFile = inputFiles.get(i);
                log.info("[{}/{}] {}", i + 1, inputFiles.size(), inputFile.getName());

                try {
                    // 加載圖片
                    BufferedImage image = ImageIO.read(inputFile);
                    if (image == null) {
                        log.error("  ERROR: Cannot read image");
                        continue;
                    }

                    log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());

                    // OCR 識別
                    log.info("  Running OCR...");
                    List<OcrService.TextBlock> textBlocks;
                    if (shouldUseTesseract(ocrEngine, language)) {
                        if (tesseractService == null) {
                            tesseractService = new TesseractOcrService(
                                config.getTesseractDataPath(), getTesseractLanguage(language));
                            log.info("  OCR Engine: Tesseract ({})", getTesseractLabel(language));
                        }
                        textBlocks = tesseractService.recognize(image);
                    } else {
                        textBlocks = ocrService.recognize(image, language);
                    }

                    // 簡繁轉換
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                        log.info("  OK: Text converted ({})", config.getTextConvert());
                    }

                    log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                    // 保存數據
                    images.add(image);
                    allTextBlocks.add(textBlocks);

                } catch (Exception e) {
                    log.error("  ERROR: {}", e.getMessage());
                }
            }

            if (images.isEmpty()) {
                log.error("ERROR: No valid images processed");
                return;
            }

            log.info("Generating multi-page output...");

            // 生成輸出文件名
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputFilename = "multipage_" + timestamp;

            // 生成多頁 PDF
            if (format.contains("pdf") || format.contains("all")) {
                File pdfFile = new File(outputDir, outputFilename + ".pdf");
                pdfService.generateMultiPagePdf(images, allTextBlocks, pdfFile);
                log.info("  OK: Multi-page PDF -> {}", pdfFile.getName());
            }

            // 生成 TXT（所有頁面的文字）
            if (format.contains("txt") || format.contains("all")) {
                File txtFile = new File(outputDir, outputFilename + ".txt");
                textService.generateMultiPageTxt(allTextBlocks, txtFile);
                log.info("  OK: TXT -> {}", txtFile.getName());
            }

            // 生成多頁 OFD
            if (format.contains("ofd") || format.contains("all")) {
                File ofdFile = new File(outputDir, outputFilename + ".ofd");
                ofdService.generateMultiPageOfd(images, allTextBlocks, ofdFile);
                log.info("  OK: Multi-page OFD -> {}", ofdFile.getName());
            }

            log.info("Total pages: {}", images.size());

        } catch (Exception e) {
            log.error("ERROR in multi-page processing: {}", e.getMessage(), e);
        }
    }

    /**
     * 單頁模式：每個圖片生成一個 PDF/OFD
     */
    private static void processPerPage(List<File> inputFiles, File outputDir,
                                      String format, String language, String ocrEngine, Config config,
                                      OcrService ocrService, PdfService pdfService,
                                      TextService textService, OfdService ofdService,
                                      TesseractOcrService tesseractService) {
        int processed = 0;
        int failed = 0;

        for (File inputFile : inputFiles) {
            processed++;
            log.info("[{}/{}] Processing: {}", processed, inputFiles.size(), inputFile.getName());

            try {
                // 加載圖片
                BufferedImage image = ImageIO.read(inputFile);
                if (image == null) {
                    log.error("  ERROR: Cannot read image");
                    failed++;
                    continue;
                }

                log.info("  Image size: {}x{}", image.getWidth(), image.getHeight());

                // OCR 識別
                log.info("  Running OCR...");
                List<OcrService.TextBlock> textBlocks;
                if (shouldUseTesseract(ocrEngine, language)) {
                    if (tesseractService == null) {
                        tesseractService = new TesseractOcrService(
                            config.getTesseractDataPath(), getTesseractLanguage(language));
                        log.info("  OCR Engine: Tesseract ({})", getTesseractLabel(language));
                    }
                    textBlocks = tesseractService.recognize(image);
                } else {
                    textBlocks = ocrService.recognize(image, language);
                }

                // 簡繁轉換
                if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                    convertTextBlocks(textBlocks, config.getTextConvert());
                    log.info("  OK: Text converted ({})", config.getTextConvert());
                }

                log.info("  OK: OCR completed ({} blocks)", textBlocks.size());

                // 生成輸出
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String baseName = getBaseName(inputFile.getName());
                String outputFilename = baseName + "_" + timestamp;

                // 導出文件
                if (format.contains("pdf") || format.contains("all")) {
                    File pdfFile = new File(outputDir, outputFilename + ".pdf");
                    pdfService.generatePdf(image, textBlocks, pdfFile);
                    log.info("  OK: PDF -> {}", pdfFile.getName());
                }

                if (format.contains("txt") || format.contains("all")) {
                    File txtFile = new File(outputDir, outputFilename + ".txt");
                    textService.generateTxt(textBlocks, txtFile);
                    log.info("  OK: TXT -> {}", txtFile.getName());
                }

                if (format.contains("ofd") || format.contains("all")) {
                    File ofdFile = new File(outputDir, outputFilename + ".ofd");
                    ofdService.generateOfd(image, textBlocks, ofdFile);
                    log.info("  OK: OFD -> {}", ofdFile.getName());
                }

            } catch (Exception e) {
                log.error("  ERROR: {}", e.getMessage(), e);
                failed++;
            }
        }

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
    }

    /**
     * 簡繁轉換
     * @param textBlocks OCR 識別的文字區塊
     * @param mode "s2t" (簡→繁) 或 "t2s" (繁→簡)
     */
    private static void convertTextBlocks(List<OcrService.TextBlock> textBlocks, String mode) {
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

    /**
     * PDF 轉 searchable 模式
     * 將輸入的 PDF 文件渲染為圖片，OCR 後重新生成可搜索的 PDF/OFD
     */
    private static void processPdfToSearchable(List<File> pdfFiles, File outputDir,
                                               String format, String language, String ocrEngine,
                                               float dpi, Config config,
                                               OcrService ocrService, PdfService pdfService,
                                               TextService textService, OfdService ofdService,
                                               TesseractOcrService tesseractService) {
        try {
            PdfToImagesService pdfToImages = new PdfToImagesService();

            for (int f = 0; f < pdfFiles.size(); f++) {
                File pdfFile = pdfFiles.get(f);
                log.info("[{}/{}] {}", f + 1, pdfFiles.size(), pdfFile.getName());

                // 渲染 PDF 每一頁為圖片
                List<BufferedImage> pages = pdfToImages.renderPages(pdfFile, dpi);

                // OCR 每一頁
                List<List<OcrService.TextBlock>> allTextBlocks = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    log.info("  [{}/{}] OCR...", i + 1, pages.size());
                    List<OcrService.TextBlock> textBlocks;
                    if (shouldUseTesseract(ocrEngine, language)) {
                        if (tesseractService == null) {
                            tesseractService = new TesseractOcrService(
                                    config.getTesseractDataPath(),
                                    getTesseractLanguage(language));
                        }
                        textBlocks = tesseractService.recognize(pages.get(i));
                        log.info("  OCR Engine: Tesseract ({})", getTesseractLabel(language));
                    } else {
                        textBlocks = ocrService.recognize(pages.get(i), language);
                        log.info("  OCR Engine: RapidOCR");
                    }

                    // 簡繁轉換
                    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
                        convertTextBlocks(textBlocks, config.getTextConvert());
                    }

                    allTextBlocks.add(textBlocks);
                    log.info("  OK: {} blocks", textBlocks.size());
                }

                // 生成輸出
                String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

                if (format.contains("pdf") || format.contains("all")) {
                    String outName = baseName + "_searchable_" + timestamp + ".pdf";
                    File outFile = new File(outputDir, outName);
                    pdfService.generateMultiPagePdf(pages, allTextBlocks, outFile);
                    log.info("  OK: PDF -> {}", outName);
                }
                if (format.contains("ofd") || format.contains("all")) {
                    String outName = baseName + "_searchable_" + timestamp + ".ofd";
                    File outFile = new File(outputDir, outName);
                    ofdService.generateMultiPageOfd(pages, allTextBlocks, outFile);
                    log.info("  OK: OFD -> {}", outName);
                }
                if (format.contains("txt") || format.contains("all")) {
                    String outName = baseName + "_searchable_" + timestamp + ".txt";
                    File outFile = new File(outputDir, outName);
                    textService.generateMultiPageTxt(allTextBlocks, outFile);
                    log.info("  OK: TXT -> {}", outName);
                }
            }

        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
        }
    }

    private static List<File> getInputFiles(Map<String, Object> inputConfig) {
        List<File> files = new ArrayList<>();

        if (inputConfig == null) return files;

        if (inputConfig.containsKey("file")) {
            String filePath = (String) inputConfig.get("file");
            File file = new File(filePath);
            if (file.exists()) files.add(file);
            return files;
        }

        if (inputConfig.containsKey("folder")) {
            String folderPath = (String) inputConfig.get("folder");
            String pattern = inputConfig.containsKey("pattern")
                ? (String) inputConfig.get("pattern")
                : "*.jpg";

            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                findFiles(folder, pattern, files);
            }
        }

        return files;
    }

    private static void findFiles(File folder, String pattern, List<File> files) {
        File[] fileList = folder.listFiles();
        if (fileList == null) return;

        for (File file : fileList) {
            if (file.isDirectory()) {
                findFiles(file, pattern, files);
            } else if (file.isFile() && matchesPattern(file.getName(), pattern)) {
                files.add(file);
            }
        }
    }

    private static boolean matchesPattern(String filename, String pattern) {
        if (pattern.equals("*") || pattern.equals("*.*")) return true;
        if (pattern.startsWith("*.")) {
            String ext = pattern.substring(1).toLowerCase();
            return filename.toLowerCase().endsWith(ext);
        }
        return filename.equals(pattern);
    }

    private static String getOutputFolder(Map<String, Object> outputConfig) {
        if (outputConfig != null && outputConfig.containsKey("folder")) {
            return (String) outputConfig.get("folder");
        }
        return ".";
    }

    private static String getOutputFormat(Map<String, Object> outputConfig) {
        if (outputConfig != null) {
            // 支持 "formats" 或 "format" 鍵
            Object format = null;
            if (outputConfig.containsKey("formats")) {
                format = outputConfig.get("formats");
            } else if (outputConfig.containsKey("format")) {
                format = outputConfig.get("format");
            }

            if (format != null) {
                if (format instanceof String) return (String) format;
                if (format instanceof List) return String.join(",", (List<String>) format);
            }
        }
        return "pdf";
    }

    private static String getOcrLanguage(Map<String, Object> ocrConfig) {
        if (ocrConfig != null && ocrConfig.containsKey("language")) {
            return (String) ocrConfig.get("language");
        }
        return "chinese_cht";
    }

    /**
     * 獲取多頁模式配置
     * 默認為 false（單頁模式）
     */
    private static boolean getMultiPageMode(Map<String, Object> outputConfig) {
        if (outputConfig != null && outputConfig.containsKey("multiPage")) {
            Object multiPage = outputConfig.get("multiPage");
            if (multiPage instanceof Boolean) {
                return (Boolean) multiPage;
            }
            if (multiPage instanceof String) {
                return Boolean.parseBoolean((String) multiPage);
            }
        }
        return false; // 默認為單頁模式
    }

    private static String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    // --- OCR dual-engine detection (per architecture rule) ---

    /**
     * Explicit detection for chi_tra, chi_sim, eng to ensure RapidOCR is used.
     * Per architecture rule: chi_tra, chi_sim, eng 優先使用 RapidOCR.
     */
    private static boolean isChineseOrEnglish(String language) {
        if (language == null) return false;
        String lower = language.toLowerCase();
        return lower.equals("chi_tra") || lower.equals("chi_sim") || lower.equals("eng")
            || lower.equals("chinese_cht") || lower.equals("chinese") || lower.equals("english")
            || lower.equals("zh") || lower.equals("zh-tw") || lower.equals("zh-cn")
            || lower.equals("en");
    }

    private static boolean isHebrew(String language) {
        return "he".equalsIgnoreCase(language) || "hebrew".equalsIgnoreCase(language);
    }

    private static boolean isThai(String language) {
        return "th".equalsIgnoreCase(language) || "tha".equalsIgnoreCase(language) || "thai".equalsIgnoreCase(language);
    }

    private static boolean isRussian(String language) {
        return "ru".equalsIgnoreCase(language) || "rus".equalsIgnoreCase(language) || "russian".equalsIgnoreCase(language);
    }

    private static boolean isPersian(String language) {
        return "fa".equalsIgnoreCase(language) || "fas".equalsIgnoreCase(language) || "persian".equalsIgnoreCase(language) || "farsi".equalsIgnoreCase(language);
    }

    private static boolean isArabic(String language) {
        return "ar".equalsIgnoreCase(language) || "ara".equalsIgnoreCase(language) || "arabic".equalsIgnoreCase(language);
    }

    private static boolean isUkrainian(String language) {
        return "uk".equalsIgnoreCase(language) || "ukr".equalsIgnoreCase(language) || "ukrainian".equalsIgnoreCase(language);
    }

    private static boolean isBulgarian(String language) {
        return "bg".equalsIgnoreCase(language) || "bul".equalsIgnoreCase(language) || "bulgarian".equalsIgnoreCase(language);
    }

    private static boolean isSerbian(String language) {
        return "sr".equalsIgnoreCase(language) || "srp".equalsIgnoreCase(language) || "serbian".equalsIgnoreCase(language);
    }

    private static boolean isMacedonian(String language) {
        return "mk".equalsIgnoreCase(language) || "mkd".equalsIgnoreCase(language) || "macedonian".equalsIgnoreCase(language);
    }

    private static boolean isBelarusian(String language) {
        return "be".equalsIgnoreCase(language) || "bel".equalsIgnoreCase(language) || "belarusian".equalsIgnoreCase(language);
    }

    private static boolean isGreek(String language) {
        return "el".equalsIgnoreCase(language) || "ell".equalsIgnoreCase(language) || "gre".equalsIgnoreCase(language) || "greek".equalsIgnoreCase(language) || "grc".equalsIgnoreCase(language);
    }

    private static boolean isHindi(String language) {
        return "hi".equalsIgnoreCase(language) || "hin".equalsIgnoreCase(language) || "hindi".equalsIgnoreCase(language);
    }

    private static boolean isGujarati(String language) {
        return "gu".equalsIgnoreCase(language) || "guj".equalsIgnoreCase(language) || "gujarati".equalsIgnoreCase(language);
    }

    private static boolean isBengali(String language) {
        return "bn".equalsIgnoreCase(language) || "ben".equalsIgnoreCase(language) || "bengali".equalsIgnoreCase(language);
    }

    private static boolean isTamil(String language) {
        return "ta".equalsIgnoreCase(language) || "tam".equalsIgnoreCase(language) || "tamil".equalsIgnoreCase(language);
    }

    private static boolean isTelugu(String language) {
        return "te".equalsIgnoreCase(language) || "tel".equalsIgnoreCase(language) || "telugu".equalsIgnoreCase(language);
    }

    private static boolean isMarathi(String language) {
        return "mr".equalsIgnoreCase(language) || "mar".equalsIgnoreCase(language) || "marathi".equalsIgnoreCase(language);
    }

    private static boolean isUrdu(String language) {
        return "ur".equalsIgnoreCase(language) || "urd".equalsIgnoreCase(language) || "urdu".equalsIgnoreCase(language);
    }

    private static boolean isPashto(String language) {
        return "ps".equalsIgnoreCase(language) || "pus".equalsIgnoreCase(language) || "pashto".equalsIgnoreCase(language);
    }

    private static boolean isAmharic(String language) {
        return "am".equalsIgnoreCase(language) || "amh".equalsIgnoreCase(language) || "amharic".equalsIgnoreCase(language);
    }

    private static boolean useTesseract(String language) {
        return isHebrew(language) || isThai(language) || isRussian(language) || isPersian(language) || isArabic(language) || isUkrainian(language) || isBulgarian(language) || isSerbian(language) || isMacedonian(language) || isBelarusian(language) || isGreek(language) || isHindi(language) || isGujarati(language) || isBengali(language) || isTamil(language) || isTelugu(language) || isMarathi(language) || isUrdu(language) || isPashto(language) || isAmharic(language);
    }

    private static boolean shouldUseTesseract(String engine, String language) {
        if ("tesseract".equals(engine)) return true;
        if ("rapidocr".equals(engine)) return false;
        // Explicit RapidOCR detection for chi_tra, chi_sim, eng (per architecture rule)
        if (isChineseOrEnglish(language)) return false;
        return useTesseract(language);
    }

    private static String getTesseractLanguage(String language) {
        if (isHebrew(language)) return "heb+eng";
        if (isThai(language)) return "tha+eng";
        if (isRussian(language)) return "rus+eng";
        if (isPersian(language)) return "ara+eng";
        if (isArabic(language)) return "ara+eng";
        if (isUkrainian(language)) return "ukr+eng";
        if (isBulgarian(language)) return "bul+eng";
        if (isSerbian(language)) return "srp+eng";
        if (isMacedonian(language)) return "mkd+eng";
        if (isBelarusian(language)) return "bel+eng";
        if (isGreek(language)) return "ell+eng";
        if (isHindi(language)) return "hin+eng";
        if (isGujarati(language)) return "guj+eng";
        if (isBengali(language)) return "ben+eng";
        if (isTamil(language)) return "tam+eng";
        if (isTelugu(language)) return "tel+eng";
        if (isMarathi(language)) return "mar+eng";
        if (isUrdu(language)) return "urd+eng";
        if (isPashto(language)) return "pus+eng";
        if (isAmharic(language)) return "amh+eng";
        return "eng";
    }

    private static String getTesseractLabel(String language) {
        if (isHebrew(language)) return "Hebrew";
        if (isThai(language)) return "Thai";
        if (isRussian(language)) return "Russian";
        if (isPersian(language)) return "Persian";
        if (isArabic(language)) return "Arabic";
        if (isUkrainian(language)) return "Ukrainian";
        if (isBulgarian(language)) return "Bulgarian";
        if (isSerbian(language)) return "Serbian";
        if (isMacedonian(language)) return "Macedonian";
        if (isBelarusian(language)) return "Belarusian";
        if (isGreek(language)) return "Greek";
        if (isHindi(language)) return "Hindi";
        if (isGujarati(language)) return "Gujarati";
        if (isBengali(language)) return "Bengali";
        if (isTamil(language)) return "Tamil";
        if (isTelugu(language)) return "Telugu";
        if (isMarathi(language)) return "Marathi";
        if (isUrdu(language)) return "Urdu";
        if (isPashto(language)) return "Pashto";
        if (isAmharic(language)) return "Amharic";
        return language;
    }

    private static void printUsage() {
        log.info("");
        log.info("========================================");
        log.info("  JPEG2PDF-OFD v{}", VERSION);
        log.info("  Pure Java SE - No Spring Boot");
        log.info("========================================");
        log.info("");
        log.info("Usage:");
        log.info("  java -jar jpeg2pdf-ofd-nospring.jar config.json");
        log.info("");
        log.info("Options:");
        log.info("  --help     Show help");
        log.info("  --version  Show version");
        log.info("");
        log.info("Config example (per-page mode):");
        log.info("  {{");
        log.info("    \"input\": {{");
        log.info("      \"folder\": \"C:/OCR/Watch\",");
        log.info("      \"pattern\": \"*.jpg\"");
        log.info("    }},");
        log.info("    \"output\": {{");
        log.info("      \"folder\": \"C:/OCR/Output\",");
        log.info("      \"format\": \"pdf\",");
        log.info("      \"multiPage\": false");
        log.info("    }},");
        log.info("    \"ocr\": {{");
        log.info("      \"language\": \"chinese_cht\"");
        log.info("    }}");
        log.info("  }}");
        log.info("");
        log.info("Config example (multi-page mode):");
        log.info("  {{");
        log.info("    \"input\": {{");
        log.info("      \"folder\": \"C:/OCR/Watch\",");
        log.info("      \"pattern\": \"*.jpg\"");
        log.info("    }},");
        log.info("    \"output\": {{");
        log.info("      \"folder\": \"C:/OCR/Output\",");
        log.info("      \"format\": \"all\",");
        log.info("      \"multiPage\": true");
        log.info("    }},");
        log.info("    \"ocr\": {{");
        log.info("      \"language\": \"chinese_cht\"");
        log.info("    }}");
        log.info("  }}");
        log.info("");
        log.info("Output modes:");
        log.info("  multiPage: false - Each image generates one PDF/OFD (default)");
        log.info("  multiPage: true  - All images merged into one multi-page PDF/OFD");
        log.info("");
    }
}
