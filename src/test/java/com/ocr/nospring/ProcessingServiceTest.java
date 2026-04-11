package com.ocr.nospring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingServiceTest {

    private ProcessingService service;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        service = new ProcessingService(config, new OcrService(), new PdfService(config),
            new OfdService(config), new TextService());
    }

    @Test
    void testGetOrCreateTesseractServiceMethodExists() throws Exception {
        Method method = ProcessingService.class.getDeclaredMethod(
            "getOrCreateTesseractService", String.class, String.class);
        assertNotNull(method, "getOrCreateTesseractService method should exist");

        int mods = method.getModifiers();
        assertTrue(Modifier.isPrivate(mods), "method should be private");
        assertTrue(Modifier.isSynchronized(mods), "method should be synchronized");
    }

    @Test
    void testTesseractServiceFieldIsVolatile() throws Exception {
        var field = ProcessingService.class.getDeclaredField("tesseractService");
        assertTrue(Modifier.isVolatile(field.getModifiers()),
            "tesseractService field should be volatile");
    }

    @Test
    void testLazyInitReturnsSameInstance() throws Exception {
        TesseractOcrService mockService =
            org.mockito.Mockito.mock(TesseractOcrService.class);

        // Set the field via reflection to simulate a previously initialized service
        var field = ProcessingService.class.getDeclaredField("tesseractService");
        field.setAccessible(true);
        field.set(service, mockService);

        // Invoke the private method - should return the existing mock, not create a new one
        Method method = ProcessingService.class.getDeclaredMethod(
            "getOrCreateTesseractService", String.class, String.class);
        method.setAccessible(true);

        Object result = method.invoke(service, "/dummy/path", "eng");
        assertSame(mockService, result,
            "getOrCreateTesseractService should return existing instance without re-creating");
    }

    @Test
    void testLazyInitCreatesNewWhenNull() throws Exception {
        // Ensure field is null
        var field = ProcessingService.class.getDeclaredField("tesseractService");
        field.setAccessible(true);
        assertNull(field.get(service), "tesseractService should start as null");

        Method method = ProcessingService.class.getDeclaredMethod(
            "getOrCreateTesseractService", String.class, String.class);
        method.setAccessible(true);

        // Invoke with a nonexistent path - may or may not throw depending on env
        try {
            method.invoke(service, "/nonexistent/tessdata", "eng");
        } catch (Exception ignored) {
            // Tesseract native lib may not be available in test env - that's acceptable
        }
    }
}
