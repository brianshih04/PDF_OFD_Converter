package com.ocr.nospring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextServiceTest {

    @TempDir
    Path tempDir;

    private TextBlock makeBlock(String text) {
        TextBlock tb = new TextBlock();
        tb.setText(text);
        tb.setX(0);
        tb.setY(0);
        tb.setWidth(100);
        tb.setHeight(20);
        return tb;
    }

    @Test
    void generateTxt_writesAllBlocks() throws Exception {
        TextService service = new TextService();
        File output = tempDir.resolve("out.txt").toFile();

        List<TextBlock> blocks = List.of(makeBlock("Hello"), makeBlock("World"));
        service.generateTxt(blocks, output);

        assertTrue(output.exists());
        String content = java.nio.file.Files.readString(output.toPath());
        assertTrue(content.contains("Hello"));
        assertTrue(content.contains("World"));
    }

    @Test
    void generateTxt_skipsEmptyBlocks() throws Exception {
        TextService service = new TextService();
        File output = tempDir.resolve("out.txt").toFile();

        List<TextBlock> blocks = List.of(makeBlock("Visible"), makeBlock(""), makeBlock("   "));
        service.generateTxt(blocks, output);

        String content = java.nio.file.Files.readString(output.toPath());
        assertTrue(content.contains("Visible"));
        assertFalse(content.contains("\r\n\r\n"));
    }

    @Test
    void generateTxt_outputIsUtf8() throws Exception {
        TextService service = new TextService();
        File output = tempDir.resolve("out.txt").toFile();

        List<TextBlock> blocks = List.of(makeBlock("繁體中文測試"));
        service.generateTxt(blocks, output);

        byte[] bytes = java.nio.file.Files.readAllBytes(output.toPath());
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("繁體中文測試"));
    }

    @Test
    void multiPage_writesPageHeaders() throws Exception {
        TextService service = new TextService();
        File output = tempDir.resolve("multi.txt").toFile();

        service.openMultiPage(output);
        service.addPage(List.of(makeBlock("Page1")), 1);
        service.addPage(List.of(makeBlock("Page2")), 2);
        service.closeMultiPage();

        String content = java.nio.file.Files.readString(output.toPath());
        assertTrue(content.contains("Page 1"));
        assertTrue(content.contains("Page 2"));
        assertTrue(content.contains("Page1"));
        assertTrue(content.contains("Page2"));
    }
}
