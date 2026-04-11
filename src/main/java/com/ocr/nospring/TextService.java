package com.ocr.nospring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文本服務 - 無 Spring Boot
 */
public class TextService {

    private static final Logger log = LoggerFactory.getLogger(TextService.class);
    
    /**
     * 生成多頁 TXT (batch API — kept for backward compatibility)
     */
    public void generateMultiPageTxt(List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {
        openMultiPage(outputFile);
        try {
            for (int i = 0; i < allTextBlocks.size(); i++) {
                addPage(allTextBlocks.get(i), i + 1);
            }
        } finally {
            closeMultiPage();
        }
    }

    /**
     * Open a new multi-page TXT. Caller must call addPage() then closeMultiPage().
     */
    public void openMultiPage(File outputFile) throws Exception {
        this.multiPageWriter = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(outputFile), StandardCharsets.UTF_8));
        this.multiPageCount = 0;
    }

    /**
     * Add text blocks for one page to an open multi-page TXT.
     */
    public void addPage(List<TextBlock> textBlocks, int pageNumber) throws Exception {
        if (multiPageWriter == null) throw new IllegalStateException("Multi-page TXT not open");

        if (multiPageCount > 0) {
            multiPageWriter.newLine();
            multiPageWriter.write("========================================");
            multiPageWriter.newLine();
        }
        multiPageWriter.write("Page " + pageNumber);
        multiPageWriter.newLine();
        multiPageWriter.newLine();

        for (TextBlock block : textBlocks) {
            String text = block.getText();
            if (text != null && !text.trim().isEmpty()) {
                multiPageWriter.write(text);
                multiPageWriter.newLine();
            }
        }
        multiPageCount++;
    }

    /**
     * Finalize and save a multi-page TXT.
     */
    public void closeMultiPage() throws Exception {
        if (multiPageWriter != null) {
            multiPageWriter.close();
            multiPageWriter = null;
        }
    }

    private BufferedWriter multiPageWriter;
    private int multiPageCount;
    
    public void generateTxt(List<TextBlock> textBlocks, File outputFile) throws Exception {
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (TextBlock block : textBlocks) {
                String text = block.getText();
                if (text != null && !text.trim().isEmpty()) {
                    writer.write(text);
                    writer.newLine();
                }
            }
        }
    }
}
