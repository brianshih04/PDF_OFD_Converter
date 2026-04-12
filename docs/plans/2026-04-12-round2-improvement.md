# PDF-OFD-Converter 第二輪改善計畫

> **分支**: `her_dev`（基於 commit `025261b`）
> **日期**: 2026-04-12
> **前置條件**: P0 × 2 + P1 × 4 + P2 × 5 已全部完成（11 commits）

---

## P3 — 消除程式碼重複（DRY）

### P3-1: ProcessingService 抽取 `runOcr()` + `applyTextConversion()` 共用方法

**現狀**: 3 個 process 方法中 OCR 引擎選擇邏輯（`shouldUseTesseract` → `getOrCreateTesseractService` / `ocrService.recognize`）+ textConvert 邏輯各重複 3 次，每次約 12 行。

**方案**:
```java
private List<TextBlock> runOcr(BufferedImage image, String language, String ocrEngine) throws Exception {
    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
        tesseractService = getOrCreateTesseractService(
            config.getTesseractDataPath(), TesseractLanguageHelper.getTesseractLanguage(language));
        return tesseractService.recognize(image);
    }
    return ocrService.recognize(image, language);
}

private void applyTextConversion(List<TextBlock> textBlocks) {
    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
        convertTextBlocks(textBlocks, config.getTextConvert());
        log.info("  OK: Text converted ({})", config.getTextConvert());
    }
}
```

**影響**: ProcessingService.java — 減少 ~60 行重複

**檔案**:
- `src/main/java/com/ocr/nospring/ProcessingService.java`

---

### P3-2: 抽取 FormatSet 解析工具，消除 `format.contains()` 重複

**現狀**: `format.contains("pdf")`、`format.contains("txt")`、`format.contains("ofd")`、`format.contains("all")` 共出現 9 次（3 個 process 方法 × 3 種格式）。

**方案**: 在 process 方法入口一次解析為 Set，之後用 Set.contains：
```java
private Set<String> parseFormats(String format) {
    String normalized = (format == null || format.isEmpty()) ? "pdf" : format.toLowerCase().trim();
    if (normalized.equals("all")) return Set.of("pdf", "txt", "ofd");
    return new HashSet<>(Arrays.asList(normalized.split(",")));
}
```

**影響**: ProcessingService.java — 9 處 `format.contains()` → `formats.contains()`，語意更清晰

**檔案**:
- `src/main/java/com/ocr/nospring/ProcessingService.java`

---

### P3-3: Main.java helper 方法移入 ConfigLoader

**現狀**: Main.java 仍有 ~150 行 helper 方法（getInputFiles, findFiles, matchesPattern, getOutputFolder, getOutputFormat, getMultiPageMode），這些本質上是 config 解析的一部分。

**方案**: 將 6 個 static helper 方法移到 ConfigLoader，Main.java 只保留 CLI 編排（讀 config → 驗證 → 委派 ProcessingService）。

**影響**:
- Main.java 355 → ~200 行
- ConfigLoader 增加 ~150 行（靜態工具方法）
- 相關測試跟著遷移

**檔案**:
- `src/main/java/com/ocr/nospring/Main.java`
- `src/main/java/com/ocr/nospring/ConfigLoader.java`

---

## P4 — 記憶體與效能

### P4-1: PdfService 圖片編碼優化

**現狀**: `addPage()` 和 `generatePdf()` 每頁都執行 `ImageIO.write(image, "PNG", ByteArrayOutputStream)`。對於大尺寸照片（如 300 DPI A4 = 2550×3300），PNG 無損壓縮極慢且記憶體佔用高。

**方案**:
1. 智慧格式選擇：若圖像非透明（大多數掃描檔），用 JPEG 壓縮（品質 0.85）取代 PNG
2. 多頁模式中重用 ByteArrayOutputStream（reset 而非新建）

```java
private byte[] encodeImage(BufferedImage image) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(256 * 1024);
    // 掃描圖片通常無透明通道，JPEG 更快更小
    if (!image.getColorModel().hasAlpha()) {
        ImageIO.write(image, "jpg", baos);
    } else {
        ImageIO.write(image, "png", baos);
    }
    return baos.toByteArray();
}
```

**影響**: PdfService.java — 多頁大圖處理速度提升 2-5 倍，記憶體降低

**風險**: JPEG 為破壞性壓縮，但此圖片僅作為 PDF 背景層，OCR 文字層不受影響

**檔案**:
- `src/main/java/com/ocr/nospring/PdfService.java`

---

### P4-2: PdfToImagesService 改為逐一渲染

**現狀**: `renderPages()` 一次將 PDF 所有頁面渲染為 `List<BufferedImage>`。對於大型 PDF（如 100 頁 @ 300 DPI），全部載入會佔用數 GB 記憶體。

**方案**: 提供 streaming API，每次只渲染一頁：

```java
public interface PageConsumer {
    void accept(int pageIndex, BufferedImage pageImage) throws Exception;
}

public void renderPagesStreaming(File pdfFile, float dpi, PageConsumer consumer) throws IOException {
    try (PDDocument document = PDDocument.load(pdfFile)) {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, dpi);
            consumer.accept(i, image);
            image.flush();
        }
    }
}
```

**影響**: PdfToImagesService.java + ProcessingService.processPdfToSearchable() — 記憶體使用從 O(n) 降為 O(1)

**檔案**:
- `src/main/java/com/ocr/nospring/PdfToImagesService.java`
- `src/main/java/com/ocr/nospring/ProcessingService.java`

---

### P4-3: OfdService AWT Font 快取

**現狀**: P1-1 新增的 `loadAwtFont()` 在 `renderTextBlocks()` 中每個 TextBlock 都被呼叫（因 fontSizePt 不同），但 `Font.createFont()` 的 TRUETYPE_FONT 載入是昂貴的磁碟 I/O。

**方案**: 快取 base font 實例，只對 deriveFont 做快取：

```java
private java.awt.Font cachedBaseFont;
private String cachedFontPath;

private java.awt.Font getBaseFont() {
    String fontPath = config.getFontPath();
    if (cachedBaseFont != null && Objects.equals(fontPath, cachedFontPath)) {
        return cachedBaseFont;
    }
    cachedFontPath = fontPath;
    if (fontPath != null && !fontPath.isEmpty()) {
        try {
            cachedBaseFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File(fontPath));
            return cachedBaseFont;
        } catch (Exception e) {
            log.warn("    Failed to load configured font for OFD: {}, falling back to SERIF", fontPath);
        }
    }
    cachedBaseFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1);
    return cachedBaseFont;
}
```

`loadAwtFont()` 改為 `getBaseFont().deriveFont(fontSizePt)`。

**影響**: OfdService.java — 每個文件僅載入一次字型（而非每個 TextBlock 載入一次）

**檔案**:
- `src/main/java/com/ocr/nospring/OfdService.java`

---

## P5 — 穩定性加固

### P5-1: ProcessingService 抽取輸出管理 template method

**現狀**: 3 個 process 方法的 open/close output 文件邏輯（pdfDoc, txtOpen, ofdOpen）+ finally 清理各重複一次，每次 ~30 行。若新增輸出格式需改 3 處。

**方案**: 抽取 `OutputSession` 內部類，封裝 open/addPage/close 生命週期：

```java
private class OutputSession implements AutoCloseable {
    PDDocument pdfDoc;
    boolean pdfOpen, txtOpen, ofdOpen;

    void open(File pdfFile, File txtFile, File ofdFile) throws Exception { ... }
    void addPage(BufferedImage image, List<TextBlock> textBlocks, int pageNum) throws Exception { ... }
    List<String> close() throws Exception { ... }
    @Override void close() { /* 安全清理 */ }
}
```

**影響**: ProcessingService.java — 3 處 open/add/close 重複邏輯合併為一處

**風險**: 中等 — 需仔細驗證 finally 清理邏輯不被破壞

**檔案**:
- `src/main/java/com/ocr/nospring/ProcessingService.java`

---

### P5-2: OcrService 暫存檔清理加固

**現狀**: `tempFile.delete()` 失敗僅 log.warn，長時間執行大量圖片時暫存檔會在 `%TEMP%` 堆積。

**方案**:
1. 建立 tempFile 後立即 `deleteOnExit()` 作為保底
2. finally 中仍嘗試立即刪除
3. 新增累積計數器，每 100 次刪除失敗 log 一次 summary

```java
tempFile = File.createTempFile("ocr_", ".png");
tempFile.deleteOnExit();
// ...
private static final AtomicInteger deleteFailCount = new AtomicInteger(0);
// in finally:
if (!tempFile.delete()) {
    int fails = deleteFailCount.incrementAndGet();
    if (fails % 100 == 0) {
        log.warn("Temp file delete failures accumulated: {}", fails);
    }
}
```

**影響**: OcrService.java — 防止暫存檔無限堆積

**檔案**:
- `src/main/java/com/ocr/nospring/OcrService.java`

---

### P5-3: Config.getFontPath() thread-safe 延遲初始化

**現狀**: `getFontPath()` 用 null check 延遲 init，非 volatile、非 synchronized。目前單線程無問題，但 P4-2 streaming 架構或未來平行 OCR 會觸發 race。

**方案**:
```java
private volatile String fontPath;

public String getFontPath() {
    if (fontPath == null) {
        synchronized (this) {
            if (fontPath == null) {
                fontPath = detectDefaultFontPath();
            }
        }
    }
    return fontPath;
}
```

**影響**: Config.java — 雙重檢查鎖定，無效能損失

**檔案**:
- `src/main/java/com/ocr/nospring/Config.java`

---

## 執行順序

```
P3-1 → P3-2 → P3-3 → P4-1 → P4-2 → P4-3 → P5-1 → P5-2 → P5-3
 │       │       │       │       │       │       │       │       │
 ├───────┴───────┤       │       │       │       │       │       │
  消除重複                │       │       │       │       │       │
                         ├───────┴───────┤       │       │       │
                          效能優化                │       │       │
                                                 ├───────┴───────┤
                                                  穩定性加固
```

每個 Task 獨立 commit，確保可逐項 revert。

**預計總影響**:
- ProcessingService.java: -150 行重複
- Main.java: -150 行（helper 移出）
- 效能: 多頁 PDF 2-5x 提升，大型 PDF 記憶體 O(n)→O(1)
- 0 功能變更，純重構 + 效能 + 穩定性
