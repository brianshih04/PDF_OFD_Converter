package com.ocr.nospring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Loads and parses a Java CLI config.json file into a {@link Config} object.
 * Uses Jackson to deserialize the JSON, replacing the manual Map cast logic
 * previously inlined in Main.java.
 */
public final class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ConfigLoader() {}

    /**
     * Load config from a JSON file and return a fully populated Config object.
     *
     * @param configFile the config.json file
     * @return populated Config
     */
    public static Config load(File configFile) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = MAPPER.readValue(configFile, Map.class);

        Config config = new Config();

        applyFontConfig(root, config);
        applyTextLayerConfig(root, config);
        applyTextConvert(root, config);
        applyInputConfig(root, config);
        applyOcrConfig(root, config);

        return config;
    }

    static void applyFontConfig(Map<String, Object> root, Config config) {
        Object raw = root.get("font");
        if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> section = (Map<String, Object>) raw;
            if (section.containsKey("path")) {
                config.setFontPath((String) section.get("path"));
                log.info("Font: {}", config.getFontPath());
            }
        }
    }

    static void applyTextLayerConfig(Map<String, Object> root, Config config) {
        Object raw = root.get("textLayer");
        if (!(raw instanceof Map)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> section = (Map<String, Object>) raw;

        if (section.containsKey("color")) {
            config.setTextLayerColor((String) section.get("color"));
        }
        if (section.containsKey("red")) {
            config.setTextLayerRed(((Number) section.get("red")).intValue());
        }
        if (section.containsKey("green")) {
            config.setTextLayerGreen(((Number) section.get("green")).intValue());
        }
        if (section.containsKey("blue")) {
            config.setTextLayerBlue(((Number) section.get("blue")).intValue());
        }
        if (section.containsKey("opacity")) {
            config.setTextLayerOpacity(((Number) section.get("opacity")).doubleValue());
        }
    }

    static void applyTextConvert(Map<String, Object> root, Config config) {
        if (root.containsKey("textConvert")) {
            String val = (String) root.get("textConvert");
            config.setTextConvert(val);
            log.info("Text Convert: {}", val);
        }
    }

    @SuppressWarnings("unchecked")
    static void applyInputConfig(Map<String, Object> root, Config config) {
        Map<String, Object> section = (Map<String, Object>) root.get("input");
        if (section == null) return;

        if (section.containsKey("dpi")) {
            config.setDpi(((Number) section.get("dpi")).intValue());
        }
    }

    @SuppressWarnings("unchecked")
    static void applyOcrConfig(Map<String, Object> root, Config config) {
        Map<String, Object> section = (Map<String, Object>) root.get("ocr");
        if (section == null) return;

        if (section.containsKey("language")) {
            config.setOcrLanguage((String) section.get("language"));
        }
        if (section.containsKey("tesseractDataPath")) {
            config.setTesseractDataPath((String) section.get("tesseractDataPath"));
        }
    }

    /**
     * Extract input type string from config map.
     */
    @SuppressWarnings("unchecked")
    public static String getInputType(Map<String, Object> root) {
        Map<String, Object> input = (Map<String, Object>) root.get("input");
        if (input != null && input.containsKey("type")) {
            return ((String) input.get("type")).toLowerCase();
        }
        return "image";
    }

    /**
     * Extract DPI for PDF rendering (default 300).
     */
    @SuppressWarnings("unchecked")
    public static float getRenderDpi(Map<String, Object> root) {
        Map<String, Object> input = (Map<String, Object>) root.get("input");
        if (input != null && input.containsKey("dpi")) {
            return ((Number) input.get("dpi")).floatValue();
        }
        return 300f;
    }

    /**
     * Extract OCR engine selection (default "auto").
     */
    @SuppressWarnings("unchecked")
    public static String getOcrEngine(Map<String, Object> root) {
        Map<String, Object> ocr = (Map<String, Object>) root.get("ocr");
        if (ocr != null && ocr.containsKey("engine")) {
            return ((String) ocr.get("engine")).toLowerCase();
        }
        return "auto";
    }

    /**
     * Parse config file into a raw Map (kept for backward-compatible helper access).
     */
    public static Map<String, Object> loadRaw(File configFile) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = MAPPER.readValue(configFile, Map.class);
        return root;
    }
}
