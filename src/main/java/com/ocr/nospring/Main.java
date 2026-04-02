package com.ocr.nospring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 純 Java SE 主程序 - 無 Spring Boot
 *
 * 支持兩種輸出模式：
 * 1. perPage (默認) - 每個圖片生成一個 PDF/OFD
 * 2. multiPage - 所有圖片合併成一個多頁 PDF/OFD
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static String VERSION = "0.20";
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
            // No args = print usage
            if (args.length == 0) {
                printUsage();
                System.exit(0);
            }

            System.setProperty("java.awt.headless", "true");

            if (args[0].equals("--help") || args[0].equals("-h")) {
                printUsage();
                System.exit(0);
            }

            if (args[0].equals("--version") || args[0].equals("-v")) {
                log.info("{} v{}", APP_NAME, VERSION);
                System.exit(0);
            }

            // 創建配置
            Config config = new Config();

            // 加載配置文件
            String configFile = args[0];
            Path safeConfigPath;
            try {
                safeConfigPath = PathValidator.sanitize(configFile);
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Invalid config path: {}", e.getMessage());
                System.exit(1);
                return;
            }
            File file = safeConfigPath.toFile();
            if (!file.exists()) {
                log.error("ERROR: Config file not found: {}", configFile);
                log.error("Please check the path or try one of the following .json files in current directory:");

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

            // === 讀取所有配置到 Config 物件 ===

            // 讀取字體配置 (NPE-safe cast)
            Object fontRaw = configMap.get("font");
            if (fontRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fontConfig = (Map<String, Object>) fontRaw;
                if (fontConfig.containsKey("path")) {
                    config.setFontPath((String) fontConfig.get("path"));
                    log.info("Font: {}", config.getFontPath());
                }
            }

            // 讀取文字層配置 (NPE-safe cast)
            Object textLayerRaw = configMap.get("textLayer");
            if (textLayerRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> textLayerConfig = (Map<String, Object>) textLayerRaw;
                if (textLayerConfig.containsKey("color")) {
                    config.setTextLayerColor((String) textLayerConfig.get("color"));
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

            // 讀取輸入配置
            @SuppressWarnings("unchecked")
            Map<String, Object> inputConfig = (Map<String, Object>) configMap.get("input");
            @SuppressWarnings("unchecked")
            Map<String, Object> outputConfig = (Map<String, Object>) configMap.get("output");
            @SuppressWarnings("unchecked")
            Map<String, Object> ocrConfig = (Map<String, Object>) configMap.get("ocr");

            // 獲取輸入類型
            String inputType = "image";
            if (inputConfig != null && inputConfig.containsKey("type")) {
                inputType = ((String) inputConfig.get("type")).toLowerCase();
            }

            // 獲取 DPI（PDF 渲染用，預設 300）
            float renderDpi = 300f;
            if (inputConfig != null && inputConfig.containsKey("dpi")) {
                renderDpi = ((Number) inputConfig.get("dpi")).floatValue();
            }

            // 獲取 OCR 語言與引擎
            String language = getOcrLanguage(ocrConfig);
            config.setOcrLanguage(language);

            String ocrEngine = "auto";
            if (ocrConfig != null && ocrConfig.containsKey("engine")) {
                ocrEngine = ((String) ocrConfig.get("engine")).toLowerCase();
            }
            log.info("OCR Engine: {}", ocrEngine);

            // 讀取 Tesseract tessdata 路徑
            if (ocrConfig != null && ocrConfig.containsKey("tesseractDataPath")) {
                config.setTesseractDataPath((String) ocrConfig.get("tesseractDataPath"));
            }

            // === 驗證配置（所有設定讀取完畢後） ===
            try {
                config.validate();
                log.info("OK: Config validated");
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Configuration validation failed: {}", e.getMessage());
                System.exit(1);
            }

            log.info("Configuration loaded and validated from: {}", configFile);

            // === 獲取輸入檔案 ===
            List<File> inputFiles = getInputFiles(inputConfig);

            if ("pdf".equals(inputType)) {
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
            } else {
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

            // === 獲取輸出配置 ===
            String outputFolder = getOutputFolder(outputConfig);
            String format = getOutputFormat(outputConfig);
            boolean multiPage = getMultiPageMode(outputConfig);

            // Validate output path
            try {
                outputFolder = PathValidator.sanitize(outputFolder).toString();
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Invalid output path: {}", e.getMessage());
                System.exit(1);
                return;
            }

            // 創建輸出目錄
            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            log.info("Output: {}", outputFolder);
            log.info("Mode: {}", multiPage ? "Multi-Page" : "Per-Page");

            // === 委派給 ProcessingService ===
            OcrService ocrService = new OcrService();
            PdfService pdfService = new PdfService(config);
            OfdService ofdService = new OfdService(config);
            TextService textService = new TextService();
            ProcessingService processingService = new ProcessingService(config, ocrService, pdfService, ofdService, textService);

            ProcessingService.ProgressCallback callback = new CliProgressCallback();

            if ("pdf".equals(inputType)) {
                processingService.processPdfToSearchable(inputFiles, outputDir, format, language, ocrEngine, renderDpi, callback);
            } else if (multiPage) {
                processingService.processMultiPage(inputFiles, outputDir, format, language, ocrEngine, callback);
            } else {
                processingService.processPerPage(inputFiles, outputDir, format, language, ocrEngine, callback);
            }

            log.info("========================================");
            log.info("  Done!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    // ==================== Helper Methods ====================

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
                : "*.jpg,*.jpeg,*.png,*.bmp,*.tiff,*.tif";

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
        // Support comma-separated patterns: "*.jpg,*.png,*.tiff"
        if (pattern.contains(",")) {
            String[] patterns = pattern.split(",");
            for (String p : patterns) {
                p = p.trim();
                if (!p.isEmpty() && p.startsWith("*.")) {
                    String ext = p.substring(1).toLowerCase();
                    if (filename.toLowerCase().endsWith(ext)) return true;
                }
            }
            return false;
        }
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
        log.info("Output modes:");
        log.info("  multiPage: false - Each image generates one PDF/OFD (default)");
        log.info("  multiPage: true  - All images merged into one multi-page PDF/OFD");
        log.info("");
    }

    /**
     * CLI progress callback that emits structured JSON lines to stdout.
     * Python subprocess bridge parses these lines for real-time progress.
     */
    private static class CliProgressCallback implements ProcessingService.ProgressCallback {
        private final ObjectMapper mapper = new ObjectMapper();
        private final ConcurrentLinkedQueue<String> outputFileQueue = new ConcurrentLinkedQueue<>();

        private void emitJson(Map<String, Object> msg) {
            try {
                System.out.println(mapper.writeValueAsString(msg));
                System.out.flush();
            } catch (Exception e) {
                System.err.println("Failed to emit progress: " + e.getMessage());
            }
        }

        @Override
        public void onProgress(int current, int total, String message) {
            emitJson(Map.of(
                "type", "progress",
                "current", current,
                "total", total,
                "message", message
            ));
        }

        @Override
        public void onComplete(List<String> outputFiles) {
            if (outputFiles != null) {
                outputFileQueue.addAll(outputFiles);
            }
            emitJson(Map.of(
                "type", "complete",
                "files", List.copyOf(outputFileQueue)
            ));
        }

        @Override
        public void onError(String message) {
            emitJson(Map.of(
                "type", "error",
                "message", message
            ));
        }
    }
}
