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

    private static String VERSION = "0.21";
    private static String APP_NAME = "JPEG2PDF-OFD (No Spring Boot)";

    static {
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

            // Validate config file path
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

            log.info("========================================");
            log.info("  {} v{}", APP_NAME, VERSION);
            log.info("========================================");
            log.info("Config: {}", configFile);
            log.info("OK: Config loaded");

            // === Load config via ConfigLoader ===
            Map<String, Object> configMap = ConfigLoader.loadRaw(file);
            Config config = ConfigLoader.load(file);

            String inputType = ConfigLoader.getInputType(configMap);
            float renderDpi = ConfigLoader.getRenderDpi(configMap);
            String language = config.getOcrLanguage() != null ? config.getOcrLanguage() : "chinese_cht";
            String ocrEngine = ConfigLoader.getOcrEngine(configMap);
            log.info("OCR Engine: {}", ocrEngine);

            // === Validate config ===
            try {
                config.validate();
                log.info("OK: Config validated");
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Configuration validation failed: {}", e.getMessage());
                System.exit(1);
            }

            log.info("Configuration loaded and validated from: {}", configFile);

            // === Get input files ===
            @SuppressWarnings("unchecked")
            Map<String, Object> inputConfig = (Map<String, Object>) configMap.get("input");
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

            // === Get output config ===
            @SuppressWarnings("unchecked")
            Map<String, Object> outputConfig = (Map<String, Object>) configMap.get("output");
            String outputFolder = getOutputFolder(outputConfig);
            String format = getOutputFormat(outputConfig);
            boolean multiPage = getMultiPageMode(outputConfig);

            try {
                outputFolder = PathValidator.sanitize(outputFolder).toString();
            } catch (IllegalArgumentException e) {
                log.error("ERROR: Invalid output path: {}", e.getMessage());
                System.exit(1);
                return;
            }

            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            log.info("Output: {}", outputFolder);
            log.info("Mode: {}", multiPage ? "Multi-Page" : "Per-Page");

            // === Delegate to ProcessingService ===
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
        return false;
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
