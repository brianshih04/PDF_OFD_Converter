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
import java.util.concurrent.CountDownLatch;

/**
 * JavaFX GUI Application with WebView frontend.
 * Provides a web-based UI for JPEG2PDF-OFD OCR conversion.
 */
public class GuiApp extends Application {

    private static final String VERSION = "3.0.0 (GUI)";

    private WebEngine webEngine;
    private ProcessingService processingService;
    private Config config;
    private Task<Void> currentTask;
    private Stage primaryStage;
    private File lastDirectory;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.config = new Config();
        this.lastDirectory = new File(System.getProperty("user.dir"));

        WebView webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Load HTML from resources
        loadHtmlFromResources();

        // Create scene
        Scene scene = new Scene(webView, 900, 700);
        stage.setTitle("JPEG2PDF-OFD OCR v" + VERSION);
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
        try {
            InputStream is = getClass().getResourceAsStream("/web/index.html");
            if (is != null) {
                String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                webEngine.loadContent(html, "text/html");
            } else {
                webEngine.loadContent(getFallbackHtml(), "text/html");
            }
        } catch (Exception e) {
            e.printStackTrace();
            webEngine.loadContent(getFallbackHtml(), "text/html");
        }

        // Primary mechanism: use loadWorker state listener to detect when page is ready
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("LoadWorker SUCCEEDED - setting up bridge");
                setupJavaBridge();
            }
        });

        // Fallback: Setup bridge after a short delay as backup (belt and suspenders)
        // This handles cases where loadWorker might not fire SUCCEEDED consistently
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        delay.setOnFinished(e -> {
            System.out.println("PauseTransition fallback - setting up bridge");
            setupJavaBridge();
        });
        delay.play();
    }

    /**
     * Setup Java bridge for JavaScript calls.
     */
    private void setupJavaBridge() {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaApp", new JavaBridge());
            System.out.println("Java bridge initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Java Bridge class - exposed to JavaScript.
     */
    public class JavaBridge {

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
            System.out.println("openDirectoryChooser called, isFxThread=" + Platform.isFxApplicationThread());
            if (Platform.isFxApplicationThread()) {
                return doOpenDirectoryChooser();
            } else {
                final String[] result = {""};
                final CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    result[0] = doOpenDirectoryChooser();
                    latch.countDown();
                });
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return result[0];
            }
        }

        private String doOpenDirectoryChooser() {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("選擇資料夾");
            if (lastDirectory != null && lastDirectory.exists()) {
                chooser.setInitialDirectory(lastDirectory);
            }
            File selected = chooser.showDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected;
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Open file chooser dialog for PDF files.
         * Handles both FX thread and non-FX thread calls.
         */
        public String openFileChooser() {
            System.out.println("openFileChooser called, isFxThread=" + Platform.isFxApplicationThread());
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
            if (lastDirectory != null && lastDirectory.exists()) {
                chooser.setInitialDirectory(lastDirectory);
            }
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected.getParentFile();
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Start conversion process.
         * @param configJson JSON configuration string from frontend
         */
        public void startConversion(String configJson) {
            System.out.println("Starting conversion with config: " + configJson);

            // Cancel any existing task
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();
            }

            try {
                // Parse JSON config
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> configMap = mapper.readValue(configJson, Map.class);

                // Build Config object
                Config appConfig = new Config();

                // Get input type
                String inputType = (String) configMap.getOrDefault("inputType", "folder");

                // Get input path
                String inputPath = (String) configMap.get("inputPath");
                if (inputPath == null || inputPath.isEmpty()) {
                    callJsOnError("请选择输入路径");
                    return;
                }

                // Get output path
                String outputPath = (String) configMap.get("outputPath");
                if (outputPath == null || outputPath.isEmpty()) {
                    callJsOnError("请选择输出文件夹");
                    return;
                }

                File outputDir = new File(outputPath);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                // Get language
                String language = (String) configMap.getOrDefault("language", "chinese_cht");

                // Get formats
                String formats = (String) configMap.getOrDefault("formats", "pdf");

                // Get multiPage flag
                boolean multiPage = Boolean.parseBoolean(String.valueOf(configMap.getOrDefault("multiPage", false)));

                // Get input files
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

                if (inputFiles.isEmpty()) {
                    callJsOnError("未找到可处理的文件");
                    return;
                }

                System.out.println("Input type: " + inputType);
                System.out.println("Input files: " + inputFiles.size());
                System.out.println("Output: " + outputPath);
                System.out.println("Format: " + formats);
                System.out.println("Language: " + language);
                System.out.println("MultiPage: " + multiPage);

                // Create ProcessingService
                processingService = new ProcessingService(appConfig);

                // Create and run task
                final List<File> files = inputFiles;
                final String format = formats;
                final String lang = language;
                final boolean isMultiPage = multiPage;
                final String type = inputType;

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

                        if ("pdf".equals(type)) {
                            // PDF to searchable mode
                            processingService.processPdfToSearchable(files, outputDir, format, lang, "auto", 300f, callback);
                        } else if (isMultiPage) {
                            // Multi-page mode
                            processingService.processMultiPage(files, outputDir, format, lang, "auto", callback);
                        } else {
                            // Per-page mode
                            processingService.processPerPage(files, outputDir, format, lang, "auto", callback);
                        }

                        return null;
                    }
                };

                Thread thread = new Thread(currentTask);
                thread.setDaemon(true);
                thread.start();

            } catch (Exception e) {
                e.printStackTrace();
                callJsOnError("配置解析错误: " + e.getMessage());
            }
        }

        /**
         * Cancel current conversion.
         */
        public void cancelConversion() {
            if (processingService != null) {
                processingService.cancel();
                System.out.println("Conversion cancelled");
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
            System.out.println("openFontFileChooser called, isFxThread=" + Platform.isFxApplicationThread());
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
            if (lastDirectory != null && lastDirectory.exists()) {
                chooser.setInitialDirectory(lastDirectory);
            }
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Font Files", "*.ttf", "*.TTF")
            );
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected != null) {
                lastDirectory = selected.getParentFile();
                return selected.getAbsolutePath();
            }
            return "";
        }

        /**
         * Save settings to file.
         * @param settingsJson JSON string of settings
         */
        public void saveSettings(String settingsJson) {
            try {
                File settingsFile = new File(getSettingsPath());
                File settingsDir = settingsFile.getParentFile();
                if (!settingsDir.exists()) {
                    settingsDir.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
                    fos.write(settingsJson.getBytes(StandardCharsets.UTF_8));
                }
                System.out.println("Settings saved to: " + settingsFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error saving settings: " + e.getMessage());
            }
        }

        /**
         * Load settings from file.
         * @return JSON string of settings, or empty string if not exists
         */
        public String loadSettings() {
            try {
                File settingsFile = new File(getSettingsPath());
                if (!settingsFile.exists()) {
                    System.out.println("Settings file not found, using defaults");
                    return "";
                }

                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    byte[] bytes = fis.readAllBytes();
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    System.out.println("Settings loaded from: " + settingsFile.getAbsolutePath());
                    return json;
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error loading settings: " + e.getMessage());
                return "";
            }
        }

        /**
         * Delete settings file.
         */
        public void deleteSettings() {
            try {
                File settingsFile = new File(getSettingsPath());
                if (settingsFile.exists()) {
                    settingsFile.delete();
                    System.out.println("Settings file deleted");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get settings file path.
     * @return path to settings.json in user home directory
     */
    private String getSettingsPath() {
        return System.getProperty("user.home") + "/.jpeg2pdf-ofd/settings.json";
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
            e.printStackTrace();
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
            System.err.println("Error calling JS " + methodName + ": " + e.getMessage());
        }
    }

    private String escapeJs(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
