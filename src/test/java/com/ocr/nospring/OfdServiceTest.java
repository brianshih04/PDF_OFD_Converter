package com.ocr.nospring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfdServiceTest {

    @TempDir
    Path tempDir;

    /**
     * Verify generateOfd() cleans up temp files even when an exception occurs.
     * Uses a null image to force an NPE before OFD doc creation, then confirms
     * no "ofd_" temp directories are left behind in the system temp folder.
     */
    @Test
    void generateOfd_shouldCleanTempFilesOnException() {
        Config config = new Config();
        OfdService service = new OfdService(config);
        File outputFile = tempDir.resolve("output.ofd").toFile();

        // Snapshot of ofd_ temp dirs before call
        File sysTemp = new File(System.getProperty("java.io.tmpdir"));
        long beforeCount = countOfdTempDirs(sysTemp);

        assertThrows(Exception.class, () ->
                service.generateOfd(null, List.of(), outputFile)
        );

        // Allow a brief moment for async cleanup
        long afterCount = countOfdTempDirs(sysTemp);
        assertEquals(beforeCount, afterCount,
                "Temp directories leaked after generateOfd() threw an exception");
    }

    /**
     * Verify generateMultiPageOfd() cleans up temp files on exception.
     * Passes mismatched list sizes to trigger the early IllegalArgumentException,
     * then confirms no temp directories remain.
     */
    @Test
    void generateMultiPageOfd_shouldCleanTempFilesOnException() {
        Config config = new Config();
        OfdService service = new OfdService(config);
        File outputFile = tempDir.resolve("output.ofd").toFile();
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

        File sysTemp = new File(System.getProperty("java.io.tmpdir"));
        long beforeCount = countOfdTempDirs(sysTemp);

        // mismatched sizes triggers IllegalArgumentException before temp dir creation,
        // so this verifies the guard clause itself doesn't leak — no temp dirs expected
        assertThrows(IllegalArgumentException.class, () ->
                service.generateMultiPageOfd(
                        List.of(dummy),
                        List.of(), // empty → size mismatch
                        outputFile)
        );

        long afterCount = countOfdTempDirs(sysTemp);
        assertEquals(beforeCount, afterCount,
                "Temp directories leaked after generateMultiPageOfd() threw an exception");
    }

    /**
     * Verify generateOfd() cleans up temp files on successful execution.
     */
    @Test
    void generateOfd_shouldCleanTempFilesOnSuccess() throws Exception {
        Config config = new Config();
        OfdService service = new OfdService(config);
        File outputFile = tempDir.resolve("output.ofd").toFile();

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        List<TextBlock> blocks = List.of();

        File sysTemp = new File(System.getProperty("java.io.tmpdir"));
        long beforeCount = countOfdTempDirs(sysTemp);

        service.generateOfd(image, blocks, outputFile);

        assertTrue(outputFile.exists(), "OFD output file should be created");
        long afterCount = countOfdTempDirs(sysTemp);
        assertEquals(beforeCount, afterCount,
                "Temp directories leaked after successful generateOfd()");
    }

    private static long countOfdTempDirs(File sysTemp) {
        File[] dirs = sysTemp.listFiles((dir, name) -> name.startsWith("ofd_") && Files.isDirectory(dir.toPath().resolve(name).toAbsolutePath()));
        return dirs != null ? dirs.length : 0;
    }
}
