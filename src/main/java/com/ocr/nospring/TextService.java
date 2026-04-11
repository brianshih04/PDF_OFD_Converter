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
 * 文本服務 - 生成 TXT 輸出
 */
public class TextService {

    private static final Logger log = LoggerFactory.getLogger(TextService.class);

    /**
     * 生成多頁 TXT
     */
    public void generateMultiPageTxt(List<List<TextBlock>> allTextBlocks, File outputFile) throws Exception {

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (int pageIndex = 0; pageIndex < allTextBlocks.size(); pageIndex++) {
                List<TextBlock> textBlocks = allTextBlocks.get(pageIndex);

                // 添加頁面分隔符
                if (pageIndex > 0) {
                    writer.newLine();
                    writer.write("========================================");
                    writer.newLine();
                    writer.write("Page " + (pageIndex + 1));
                    writer.newLine();
                    writer.write("========================================");
                    writer.newLine();
                    writer.newLine();
                } else {
                    writer.write("Page " + (pageIndex + 1));
                    writer.newLine();
                    writer.newLine();
                }

                // 寫入文字
                writeTextBlocks(writer, textBlocks);
            }
        }
    }

    /**
     * 生成單頁 TXT
     */
    public void generateTxt(List<TextBlock> textBlocks, File outputFile) throws Exception {

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writeTextBlocks(writer, textBlocks);
        }
    }

    /**
     * 共用文字寫入方法
     */
    private void writeTextBlocks(BufferedWriter writer, List<TextBlock> textBlocks) throws java.io.IOException {
        for (TextBlock block : textBlocks) {
            String text = block.getText();
            if (text != null && !text.trim().isEmpty()) {
                writer.write(text);
                writer.newLine();
            }
        }
    }
}
