package com.ocr.nospring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper();

    private File writeConfig(Map<String, Object> config) throws Exception {
        File file = tempDir.resolve("test-config.json").toFile();
        mapper.writeValue(file, config);
        return file;
    }

    @Test
    void load_minimalConfig_setsDefaults() throws Exception {
        File file = writeConfig(Map.of(
            "input", Map.of("folder", "/tmp/in"),
            "output", Map.of("folder", "/tmp/out"),
            "ocr", Map.of("language", "chinese_cht")
        ));

        Config config = ConfigLoader.load(file);

        assertEquals("chinese_cht", config.getOcrLanguage());
        assertNull(config.getTextConvert());
        assertEquals(0.0001, config.getTextLayerOpacity(), 0.00001);
        assertEquals(255, config.getTextLayerRed());
        assertEquals(255, config.getTextLayerGreen());
        assertEquals(255, config.getTextLayerBlue());
    }

    @Test
    void load_withFontConfig_setsFontPath() throws Exception {
        File file = writeConfig(Map.of(
            "font", Map.of("path", "fonts/test.ttf")
        ));

        Config config = ConfigLoader.load(file);
        assertEquals("fonts/test.ttf", config.getFontPath());
    }

    @Test
    void load_withTextLayerColor_setsColor() throws Exception {
        File file = writeConfig(Map.of(
            "textLayer", Map.of("color", "debug")
        ));

        Config config = ConfigLoader.load(file);
        assertEquals(255, config.getTextLayerRed());
        assertEquals(0, config.getTextLayerGreen());
        assertEquals(0, config.getTextLayerBlue());
        assertEquals(1.0, config.getTextLayerOpacity(), 0.001);
    }

    @Test
    void load_withTextLayerOpacity_setsOpacity() throws Exception {
        File file = writeConfig(Map.of(
            "textLayer", Map.of("opacity", 0.5)
        ));

        Config config = ConfigLoader.load(file);
        assertEquals(0.5, config.getTextLayerOpacity(), 0.001);
    }

    @Test
    void load_withTextConvert_setsConversion() throws Exception {
        File file = writeConfig(Map.of(
            "textConvert", "s2t"
        ));

        Config config = ConfigLoader.load(file);
        assertEquals("s2t", config.getTextConvert());
    }

    @Test
    void load_withOcrEngine_setsEngine() throws Exception {
        File file = writeConfig(Map.of(
            "ocr", Map.of("engine", "tesseract", "language", "en")
        ));

        Map<String, Object> raw = ConfigLoader.loadRaw(file);
        assertEquals("tesseract", ConfigLoader.getOcrEngine(raw));
    }

    @Test
    void getInputType_defaultIsImage() throws Exception {
        File file = writeConfig(Map.of(
            "input", Map.of("folder", "/tmp/in")
        ));

        Map<String, Object> raw = ConfigLoader.loadRaw(file);
        assertEquals("image", ConfigLoader.getInputType(raw));
    }

    @Test
    void getInputType_pdfMode() throws Exception {
        File file = writeConfig(Map.of(
            "input", Map.of("type", "pdf", "file", "/tmp/test.pdf")
        ));

        Map<String, Object> raw = ConfigLoader.loadRaw(file);
        assertEquals("pdf", ConfigLoader.getInputType(raw));
    }

    @Test
    void getRenderDpi_defaultIs300() throws Exception {
        File file = writeConfig(Map.of(
            "input", Map.of("folder", "/tmp/in")
        ));

        Map<String, Object> raw = ConfigLoader.loadRaw(file);
        assertEquals(300f, ConfigLoader.getRenderDpi(raw), 0.01f);
    }

    @Test
    void getRenderDpi_customValue() throws Exception {
        File file = writeConfig(Map.of(
            "input", Map.of("folder", "/tmp/in", "dpi", 150)
        ));

        Map<String, Object> raw = ConfigLoader.loadRaw(file);
        assertEquals(150f, ConfigLoader.getRenderDpi(raw), 0.01f);
    }

    @Test
    void load_withTesseractDataPath_setsPath() throws Exception {
        File file = writeConfig(Map.of(
            "ocr", Map.of("language", "he", "tesseractDataPath", "/opt/tessdata")
        ));

        Config config = ConfigLoader.load(file);
        assertEquals("/opt/tessdata", config.getTesseractDataPath());
    }

    @Test
    void load_withRgbOverrides_setsValues() throws Exception {
        File file = writeConfig(Map.of(
            "textLayer", Map.of("red", 100, "green", 150, "blue", 200)
        ));

        Config config = ConfigLoader.load(file);
        assertEquals(100, config.getTextLayerRed());
        assertEquals(150, config.getTextLayerGreen());
        assertEquals(200, config.getTextLayerBlue());
    }
}
