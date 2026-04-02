package com.ocr.nospring;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.concurrent.CountDownLatch;

/**
 * JavaFX GUI Application with WebView frontend.
 * Provides a web-based UI for JPEG2PDF-OFD OCR conversion.
 */
public class GuiApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(GuiApp.class);
    private static String VERSION = "3.0.0 (GUI)";
    private static String APP_NAME = "JPEG2PDF-OFD-OCR";

    static {
        // Load version from properties file
        try (InputStream is = GuiApp.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String ver = props.getProperty("app.version", VERSION);
                if (ver != null && !ver.startsWith("${")) {
                    VERSION = ver + " (GUI)";
                }
                String name = props.getProperty("app.name", APP_NAME);
                if (name != null && !name.startsWith("${")) {
                    APP_NAME = name;
                }
            }
        } catch (Exception e) {
            log.warn("Could not load version.properties, using default version");
        }
    }

    private WebEngine webEngine;
    private ProcessingService processingService;
    private Config config;
    private volatile Task<Void> currentTask;
    private Stage primaryStage;
    private static final String PREFS_LAST_DIR = "lastDirectory";
    private File lastDirectory;
    private JavaBridge javaBridge;  // Strong reference to prevent GC
    private volatile boolean bridgeInitialized = false;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.config = new Config();
        this.lastDirectory = loadLastDirectory();

        WebView webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Load HTML from resources
        loadHtmlFromResources();

        // Create scene
        Scene scene = new Scene(webView, 900, 700);
        stage.setTitle(APP_NAME + " v" + VERSION);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        // Handle close
        stage.setOnCloseRequest(e -> {
            if (currentTask != null && currentTask.isRunning()) {
                if (processingService != null) {
                    processingService.cancel();
                }
                currentTask.cancel();
            }
            Platform.exit();
        });

        stage.show();
    }

    /**
     * Load HTML from classpath resources.
     */
    private void loadHtmlFromResources() {
        // Register listener BEFORE loadContent() to avoid race condition.
        // loadContent() with a string can fire SUCCEEDED synchronously on some
        // JavaFX versions — if the listener is registered after, it is missed.
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                log.debug("LoadWorker SUCCEEDED - setting up bridge");
                setupJavaBridge();
            }
        });

        try {
            InputStream is = getClass().getResourceAsStream("/web/index.html");
            if (is != null) {
                String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                webEngine.loadContent(html, "text/html");
            } else {
                webEngine.loadContent(getFallbackHtml(), "text/html");
            }
        } catch (Exception e) {
            log.error("Error loading HTML from resources", e);
            webEngine.loadContent(getFallbackHtml(), "text/html");
        }

        // Fallback: retry bridge setup if the loadWorker listener didn't fire
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
        delay.setOnFinished(e -> {
            if (!bridgeInitialized) {
                log.info("PauseTransition fallback - bridge not yet initialized, retrying");
                setupJavaBridge();
            }
        });
        delay.play();
    }

    /**
     * Setup Java bridge for JavaScript calls.
     */
    private void setupJavaBridge() {
        if (bridgeInitialized) return;
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            // Keep strong reference to prevent GC from collecting the bridge
            javaBridge = new JavaBridge();
            window.setMember("javaApp", javaBridge);
            bridgeInitialized = true;
            log.info("Java bridge initialized");
            webEngine.executeScript("loadSettings()");
        } catch (Exception e) {
            log.error("Error setting up Java bridge", e);
        }
    }

    /**
     * Package-private settings manager — handles save/load/delete of settings file.
     */
    static class SettingsManager {

        String getSettingsPath() {
            return System.getProperty("user.home") + "/.jpeg2pdf-ofd/settings.json";
        }

        void save(String settingsJson) {
            try {
                File settingsFile = new File(getSettingsPath());
                File settingsDir = settingsFile.getParentFile();
                if (!settingsDir.exists()) {
                    settingsDir.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
                    fos.write(settingsJson.getBytes(StandardCharsets.UTF_8));
                }
                log.info("Settings saved to: {}", settingsFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Error saving settings", e);
            }
        }

        String load() {
            try {
                File settingsFile = new File(getSettingsPath());
                if (!settingsFile.exists()) {
                    log.debug("Settings file not found, using defaults");
                    return "";
                }
                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    byte[] bytes = fis.readAllBytes();
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    log.info("Settings loaded from: {}", settingsFile.getAbsolutePath());
                    return json;
                }
            } catch (Exception e) {
                log.error("Error loading settings", e);
                return "";
            }
        }

        void delete() {
            try {
                File settingsFile = new File(getSettingsPath());
                if (settingsFile.exists()) {
                    settingsFile.delete();
                    log.info("Settings file deleted");
                }
            } catch (Exception e) {
                log.error("Error deleting settings", e);
            }
        }
    }

    /**
     * Java Bridge class - exposed to JavaScript.
     */
    public class JavaBridge {

        private final SettingsManager settingsManager = new SettingsManager();

        /**
         * Get application version.
         */
        public String getVersion() {
            return VERSION;
        }

        /**
         * Open directory chooser dialog.
         * Handles both FX thread and non-FX thread calls.
         */
        public String openDirectoryChooser() {
            return openDirectoryChooser("");
        }

        /**
         * Open directory chooser dialog with a current path.
         * If currentPath is provided and valid, use it as initial directory.
         * Handles both FX thread and non-FX thread calls.
         * @param currentPath the current path to start from, or empty string to use lastDirectory
         */
        public String openDirectoryChooser(String currentPath) {
            log.debug("openDirectoryChooser called with currentPath={}, isFxThread={}", currentPath, Platform.isFxApplicationThread());
            if (Platform.isFxApplicationThread()) {
                return doOpenDirectoryChooser(currentPath);
            } else {
                final String[] result = {""};
                final CountDownLatch latch = new CountDownLatch(1);
                final String path = currentPath;
                Platform.runLater(() -> {
                    result[0] = doOpenDirectoryChooser(path);
                    latch.countDown();
                });
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return result[0];
            }
        }

        private String doOpenDirectoryChooser(String currentPath) {
            log.info("doOpenDirectoryChooser: currentPath={}, primaryStage={}", currentPath, primaryStage != null ? "visible" : "NULL");
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("選擇資料夾");

            // Priority: currentPath > lastDirectory > user.home
            File initialDir = null;
            if (currentPath != null && !currentPath.isEmpty()) {
                File currentDir = new File(currentPath);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    initialDir = currentDir;
                }
            }
            if (initialDir == null && lastDirectory != null && lastDirectory.exists()) {
                initialDir = lastDirectory;
            }
            if (initialDir == null) {
                initialDir = new File(System.getProperty("user.home"));
            }

            chooser.setInitialDirectory(initialDir);
            File selected = chooser.showDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected;
                saveLastDirectory(selected);
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Open file chooser dialog for PDF files.
         * Handles both FX thread and non-FX thread calls.
         */
        public String openFileChooser() {
            log.debug("openFileChooser called, isFxThread={}", Platform.isFxApplicationThread());
            if (Platform.isFxApplicationThread()) {
                return doOpenFileChooser();
            } else {
                final String[] result = {""};
                final CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    result[0] = doOpenFileChooser();
                    latch.countDown();
                });
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return result[0];
            }
        }

        private String doOpenFileChooser() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("選擇PDF檔案");
            File dir = (lastDirectory != null && lastDirectory.exists()) ? lastDirectory : new File(System.getProperty("user.home"));
            chooser.setInitialDirectory(dir);
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected.getParentFile();
                saveLastDirectory(lastDirectory);
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Start conversion process.
         * @param configJson JSON configuration string from frontend
         */
        public void startConversion(String configJson) {
            log.info("Starting conversion with config: {}", configJson);

            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();
            }

            try {
                ParsedConfig parsed = parseConfig(configJson);

                if (parsed.inputPath == null || parsed.inputPath.isEmpty()) {
                    callJsOnError("请选择输入路径");
                    return;
                }
                if (parsed.outputPath == null || parsed.outputPath.isEmpty()) {
                    callJsOnError("请选择输出文件夹");
                    return;
                }

                // Validate and sanitize paths
                String safeInputPath;
                String safeOutputPath;
                try {
                    safeInputPath = PathValidator.sanitize(parsed.inputPath).toString();
                    safeOutputPath = PathValidator.sanitize(parsed.outputPath).toString();
                } catch (IllegalArgumentException e) {
                    callJsOnError("路径验证失败: " + e.getMessage());
                    return;
                }

                File outputDir = new File(safeOutputPath);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                List<File> inputFiles = collectInputFiles(parsed.inputType, safeInputPath);
                if (inputFiles.isEmpty()) {
                    callJsOnError("未找到可处理的文件");
                    return;
                }

                log.info("Input type: {}", parsed.inputType);
                log.info("Input files: {}", inputFiles.size());
                log.info("Output: {}", safeOutputPath);
                log.info("Format: {}", parsed.formats);
                log.info("Language: {}", parsed.language);
                log.info("MultiPage: {}", parsed.multiPage);

                createAndRunTask(parsed, inputFiles, outputDir);

            } catch (Exception e) {
                log.error("Configuration parsing error", e);
                callJsOnError("配置解析错误: " + e.getMessage());
            }
        }

        /**
         * Cancel current conversion.
         */
        public void cancelConversion() {
            if (processingService != null) {
                processingService.cancel();
                log.info("Conversion cancelled");
            }
            if (currentTask != null) {
                currentTask.cancel();
            }
            callJsOnLog("已取消转换");
        }

        /**
         * Open file chooser dialog for TTF font files.
         * Handles both FX thread and non-FX thread calls.
         * @return selected file path or empty string if cancelled
         */
        public String openFontFileChooser() {
            log.debug("openFontFileChooser called, isFxThread={}", Platform.isFxApplicationThread());
            if (Platform.isFxApplicationThread()) {
                return doOpenFontFileChooser();
            } else {
                final String[] result = {""};
                final CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    result[0] = doOpenFontFileChooser();
                    latch.countDown();
                });
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return result[0];
            }
        }

        private String doOpenFontFileChooser() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("選擇字體檔案");
            File dir = (lastDirectory != null && lastDirectory.exists()) ? lastDirectory : new File(System.getProperty("user.home"));
            chooser.setInitialDirectory(dir);
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Font Files", "*.ttf", "*.TTF")
            );
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected.getParentFile();
                saveLastDirectory(lastDirectory);
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Save settings to file.
         * @param settingsJson JSON string of settings
         */
        public void saveSettings(String settingsJson) {
            settingsManager.save(settingsJson);
        }

        /**
         * Load settings from file.
         * @return JSON string of settings, or empty string if not exists
         */
        public String loadSettings() {
            return settingsManager.load();
        }

        /**
         * Delete settings file.
         */
        public void deleteSettings() {
            settingsManager.delete();
        }
    }

    // --- Conversion helper classes and methods ---

    /**
     * Holds parsed configuration extracted from frontend JSON.
     */
    private static class ParsedConfig {
        final Config config;
        final String inputType;
        final String inputPath;
        final String outputPath;
        final String language;
        final String ocrEngine;
        final String formats;
        final boolean multiPage;

        ParsedConfig(Config config, String inputType, String inputPath, String outputPath,
                     String language, String ocrEngine, String formats, boolean multiPage) {
            this.config = config;
            this.inputType = inputType;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.language = language;
            this.ocrEngine = ocrEngine;
            this.formats = formats;
            this.multiPage = multiPage;
        }
    }

    /**
     * Parse JSON config from frontend and build Config object + extracted parameters.
     */
    private ParsedConfig parseConfig(String configJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = mapper.readValue(configJson, Map.class);

        Config appConfig = new Config();

        if (configMap.containsKey("textColor")) {
            appConfig.setTextLayerColor((String) configMap.get("textColor"));
        }
        if (configMap.containsKey("textOpacity")) {
            appConfig.setTextLayerOpacity(((Number) configMap.get("textOpacity")).doubleValue());
        }
        if (configMap.containsKey("chineseConversion") && !"null".equals(configMap.get("chineseConversion"))) {
            appConfig.setTextConvert((String) configMap.get("chineseConversion"));
        }
        if (configMap.containsKey("customFontPath")) {
            String fontPath = (String) configMap.get("customFontPath");
            if (fontPath != null && !fontPath.isEmpty()) {
                appConfig.setFontPath(fontPath);
            }
        }
        if (configMap.containsKey("tesseractDataPath")) {
            appConfig.setTesseractDataPath((String) configMap.get("tesseractDataPath"));
        }

        String language = (String) configMap.getOrDefault("language", "chinese_cht");
        appConfig.setOcrLanguage(language);

        return new ParsedConfig(
            appConfig,
            (String) configMap.getOrDefault("inputType", "folder"),
            (String) configMap.get("inputPath"),
            (String) configMap.get("outputPath"),
            language,
            (String) configMap.getOrDefault("ocrEngine", "auto"),
            (String) configMap.getOrDefault("formats", "pdf"),
            Boolean.parseBoolean(String.valueOf(configMap.getOrDefault("multiPage", false)))
        );
    }

    /**
     * Collect input files based on input type (folder or single PDF).
     */
    private List<File> collectInputFiles(String inputType, String inputPath) {
        List<File> inputFiles = new ArrayList<>();
        if ("folder".equals(inputType)) {
            File folder = new File(inputPath);
            if (folder.exists() && folder.isDirectory()) {
                findImageFiles(folder, inputFiles);
            }
        } else if ("pdf".equals(inputType)) {
            File pdfFile = new File(inputPath);
            if (pdfFile.exists() && pdfFile.isFile()) {
                inputFiles.add(pdfFile);
            }
        }
        return inputFiles;
    }

    /**
     * Create a background Task for conversion and start it on a daemon thread.
     * All UI callbacks use Platform.runLater() for thread safety.
     */
    private void createAndRunTask(ParsedConfig parsed, List<File> inputFiles, File outputDir) {
        processingService = new ProcessingService(
            parsed.config, new OcrService(), new PdfService(parsed.config),
            new OfdService(parsed.config), new TextService());

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ProcessingService.ProgressCallback callback = new ProcessingService.ProgressCallback() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        Platform.runLater(() -> callJsOnProgress(current, total, message));
                    }

                    @Override
                    public void onComplete(List<String> outputFiles) {
                        Platform.runLater(() -> callJsOnComplete(outputFiles));
                    }

                    @Override
                    public void onError(String message) {
                        Platform.runLater(() -> callJsOnError(message));
                    }
                };

                if ("pdf".equals(parsed.inputType)) {
                    processingService.processPdfToSearchable(
                        inputFiles, outputDir, parsed.formats, parsed.language, parsed.ocrEngine, 300f, callback);
                } else if (parsed.multiPage) {
                    processingService.processMultiPage(
                        inputFiles, outputDir, parsed.formats, parsed.language, parsed.ocrEngine, callback);
                } else {
                    processingService.processPerPage(
                        inputFiles, outputDir, parsed.formats, parsed.language, parsed.ocrEngine, callback);
                }

                return null;
            }
        };

        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Load lastDirectory from java.util.prefs, falling back to user.home.
     */
    private File loadLastDirectory() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(GuiApp.class);
            String path = prefs.get(PREFS_LAST_DIR, null);
            if (path != null) {
                File dir = new File(path);
                if (dir.exists()) {
                    return dir;
                }
            }
        } catch (Exception e) {
            log.warn("Error loading lastDirectory from preferences: {}", e.getMessage());
        }
        return new File(System.getProperty("user.home"));
    }

    /**
     * Persist lastDirectory to java.util.prefs.
     */
    private void saveLastDirectory(File dir) {
        if (dir == null) return;
        try {
            Preferences prefs = Preferences.userNodeForPackage(GuiApp.class);
            prefs.put(PREFS_LAST_DIR, dir.getAbsolutePath());
            prefs.sync();
        } catch (Exception e) {
            log.warn("Error saving lastDirectory to preferences: {}", e.getMessage());
        }
    }

    /**
     * Find image files in folder (recursive).
     */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff");

    private void findImageFiles(File folder, List<File> files) {
        File[] fileList = folder.listFiles();
        if (fileList == null) return;

        for (File file : fileList) {
            if (file.isDirectory()) {
                findImageFiles(file, files);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String ext : IMAGE_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        files.add(file);
                        break;
                    }
                }
            }
        }

        // Sort by name
        files.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    /**
     * Call JavaScript onProgress callback.
     */
    private void callJsOnProgress(int current, int total, String message) {
        callJsBridgeMethod("onProgress", current, total, message);
    }

    /**
     * Call JavaScript onComplete callback.
     */
    private void callJsOnComplete(List<String> outputFiles) {
        try {
            String json = new ObjectMapper().writeValueAsString(outputFiles);
            callJsBridgeMethod("onComplete", json);
        } catch (Exception e) {
            log.error("Error serializing output files", e);
        }
    }

    /**
     * Call JavaScript onError callback.
     */
    private void callJsOnError(String message) {
        callJsBridgeMethod("onError", message);
    }

    /**
     * Call JavaScript onLog callback.
     */
    private void callJsOnLog(String message) {
        callJsBridgeMethod("onLog", message);
    }

    /**
     * Generic method to call window.javaBridge methods.
     */
    private void callJsBridgeMethod(String methodName, Object... args) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("if(window.javaBridge && window.javaBridge.").append(methodName).append(") {");
            sb.append("window.javaBridge.").append(methodName).append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                Object arg = args[i];
                if (arg == null) {
                    sb.append("null");
                } else if (arg instanceof String) {
                    sb.append("'").append(escapeJs((String) arg)).append("'");
                } else if (arg instanceof Integer || arg instanceof Double) {
                    sb.append(arg);
                } else {
                    sb.append("'").append(escapeJs(arg.toString())).append("'");
                }
            }
            sb.append(");}");
            webEngine.executeScript(sb.toString());
        } catch (Exception e) {
            log.error("Error calling JS {}: {}", methodName, e.getMessage());
        }
    }

    private String escapeJs(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("</script>", "<\\/script>")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }

    private String getFallbackHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Error</title></head>" +
               "<body style='padding:20px;font-family:Arial;background:#f5f5f5;'>" +
               "<div style='background:white;padding:20px;border-radius:8px;max-width:600px;margin:50px auto;'>" +
               "<h2 style='color:#e74c3c;'>Error: Could not load UI</h2>" +
               "<p>Please ensure <code>web/index.html</code> is in resources.</p></div></body></html>";
    }

    /**
     * Launch GUI from Main class.
     */
    public static void launchGui(String[] args) {
        launch(args);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
