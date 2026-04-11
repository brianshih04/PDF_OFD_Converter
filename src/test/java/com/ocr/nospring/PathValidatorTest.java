package com.ocr.nospring;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathValidatorTest {

    // --- Null / empty / blank ---

    @Test
    void sanitize_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PathValidator.sanitize(null));
    }

    @Test
    void sanitize_empty_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PathValidator.sanitize(""));
    }

    @Test
    void sanitize_blank_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PathValidator.sanitize("   "));
    }

    // --- Path normalization (.. and .) ---

    @Test
    void sanitize_normalizesDotDot() {
        Path result = PathValidator.sanitize("folder/../output");
        // toAbsolutePath().normalize() resolves .. away
        assertTrue(result.toString().endsWith("output"),
            "Path with .. should be normalized, got: " + result);
    }

    @Test
    void sanitize_normalizesDot() {
        Path result = PathValidator.sanitize("./folder/./file.txt");
        assertTrue(result.toString().contains("folder"),
            "Path with . should be normalized, got: " + result);
        assertFalse(result.toString().contains("./"),
            "Normalized path should not contain './', got: " + result);
    }

    @Test
    void sanitize_returnsAbsolutePath() {
        Path result = PathValidator.sanitize("some/relative/path");
        assertTrue(result.isAbsolute(),
            "Sanitized path should be absolute, got: " + result);
    }

    // --- Illegal characters ---

    @Test
    void sanitize_angleBracket_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> PathValidator.sanitize("folder<name"));
    }

    @Test
    void sanitize_doubleQuote_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> PathValidator.sanitize("folder\"name"));
    }

    @Test
    void sanitize_pipe_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> PathValidator.sanitize("folder|name"));
    }

    @Test
    void sanitize_questionMark_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> PathValidator.sanitize("folder?name"));
    }

    @Test
    void sanitize_asterisk_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> PathValidator.sanitize("folder*name"));
    }

    // --- Normal valid paths ---

    @Test
    void sanitize_simplePath_returnsCorrectly() {
        Path result = PathValidator.sanitize("output");
        assertNotNull(result);
        assertTrue(result.isAbsolute());
    }

    @Test
    void sanitize_pathWithSpaces_returnsCorrectly() {
        Path result = PathValidator.sanitize("my folder/output");
        assertNotNull(result);
        assertTrue(result.toString().contains("my folder") || result.toString().contains("my%20folder"),
            "Spaces should be preserved, got: " + result);
    }

    @Test
    void sanitize_trimmedPath() {
        Path result = PathValidator.sanitize("  output  ");
        assertNotNull(result);
        assertFalse(result.getFileName().toString().contains(" "),
            "Leading/trailing whitespace should be trimmed");
    }
}
