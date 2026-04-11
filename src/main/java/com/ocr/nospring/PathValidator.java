package com.ocr.nospring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Validates and sanitizes file system paths to prevent path traversal
 * and illegal character injection.
 */
public final class PathValidator {

    private static final Logger log = LoggerFactory.getLogger(PathValidator.class);

    /**
     * Illegal characters for Windows file paths.
     * Windows forbids: < > " | ? *
     * Universal forbid: \0 (null character)
     */
    private static final Set<Character> ILLEGAL_CHARS = Set.of(
        '<', '>', '"', '|', '?', '*', '\0'
    );

    private PathValidator() {
        // Utility class — no instantiation
    }

    /**
     * Sanitize and validate a file system path string.
     *
     * @param path the raw path string to validate
     * @return an absolute, normalized Path
     * @throws IllegalArgumentException if path is null/blank, contains illegal characters,
     *                                  or cannot be converted to a Path
     */
    public static Path sanitize(String path) throws IllegalArgumentException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or blank");
        }

        String trimmed = path.trim();

        // Check for illegal characters
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (ILLEGAL_CHARS.contains(c)) {
                throw new IllegalArgumentException(
                    "Path contains illegal character: '" + c + "' at position " + i);
            }
        }

        try {
            Path normalized = Path.of(trimmed).toAbsolutePath().normalize();
            log.debug("Path sanitized: '{}' -> '{}'", path, normalized);
            return normalized;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path: " + e.getMessage(), e);
        }
    }
}
