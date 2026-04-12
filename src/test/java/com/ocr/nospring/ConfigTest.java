package com.ocr.nospring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
    }

    @Test
    void constructor_doesNotThrow() {
        assertDoesNotThrow(Config::new);
    }

    @Test
    void setTextLayerColor_debug_setsRedAndFullOpacity() {
        config.setTextLayerColor("debug");
        assertEquals(255, config.getTextLayerRed());
        assertEquals(0, config.getTextLayerGreen());
        assertEquals(0, config.getTextLayerBlue());
        assertEquals(1.0, config.getTextLayerOpacity(), 0.001);
    }

    @Test
    void setTextLayerColor_white_setsWhite() {
        config.setTextLayerColor("white");
        assertEquals(255, config.getTextLayerRed());
        assertEquals(255, config.getTextLayerGreen());
        assertEquals(255, config.getTextLayerBlue());
    }

    @Test
    void setTextLayerColor_black_setsBlack() {
        config.setTextLayerColor("black");
        assertEquals(0, config.getTextLayerRed());
        assertEquals(0, config.getTextLayerGreen());
        assertEquals(0, config.getTextLayerBlue());
    }

    @Test
    void setTextLayerColor_null_doesNothing() {
        int prevRed = config.getTextLayerRed();
        config.setTextLayerColor(null);
        assertEquals(prevRed, config.getTextLayerRed());
    }

    @Test
    void validate_validOpacity_passes() {
        config.setTextLayerOpacity(0.5);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void validate_invalidOpacity_throws() {
        config.setTextLayerOpacity(2.0);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    void validate_invalidOpacityNegative_throws() {
        config.setTextLayerOpacity(-0.1);
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    void validate_invalidTextConvert_throws() {
        config.setTextConvert("invalid");
        assertThrows(IllegalArgumentException.class, () -> config.validate());
    }

    @Test
    void validate_validS2t_passes() {
        config.setTextConvert("s2t");
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void validate_validT2s_passes() {
        config.setTextConvert("t2s");
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void setFontPath_nonEmpty_overwrites() {
        config.setFontPath("my-font.ttf");
        assertEquals("my-font.ttf", config.getFontPath());
    }

    @Test
    void setFontPath_empty_doesNotOverwrite() {
        config.setFontPath("first.ttf");
        config.setFontPath("");
        assertEquals("first.ttf", config.getFontPath());
    }

    @Test
    void setFontPath_null_doesNotOverwrite() {
        config.setFontPath("first.ttf");
        config.setFontPath(null);
        assertEquals("first.ttf", config.getFontPath());
    }

    @Test
    void getDpi_defaultIs72() {
        assertEquals(72, config.getDpi());
    }

    @Test
    void setDpi_setsValue() {
        config.setDpi(300);
        assertEquals(300, config.getDpi());
    }
}
