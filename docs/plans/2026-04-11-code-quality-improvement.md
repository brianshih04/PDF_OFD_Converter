# PDF-OFD-Converter 程式碼品質改善計畫

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 消除程式碼重複、修復執行緒安全問題、改善效能瓶頸、統一編碼規範

**Architecture:** 維持現有的 Service-oriented 架構不變，重點在：(1) 抽取共用邏輯消除重複 (2) 修正併發缺陷 (3) 優化 I/O 熱路徑 (4) 消除隱患

**Tech Stack:** Java 21, Maven, PDFBox 2.0.29, ofdrw 2.3.8, RapidOCR 0.0.7, Tess4j 5.13.0

---

## Phase 1: 消除程式碼重複（DRY 重構）

### Task 1: 抽取 TextBlock 為獨立 top-level 類別

**Objective:** 將 TextBlock 從 OcrService 的 inner class 移出，提升封裝性，為後續重構鋪路

**Files:**
- Create: `src/main/java/com/ocr/nospring/TextBlock.java`
- Modify: `src/main/java/com/ocr/nospring/OcrService.java`

**Step 1: 建立 TextBlock 類別**

```java
// src/main/java/com/ocr/nospring/TextBlock.java
package com.ocr.nospring;

/**
 * OCR 辨識結果的文字區塊
 */
public class TextBlock {
    private String text;
    private double x;
    private double y;
    private double width;
    private double height;
    private double confidence;
    private float fontSize;

    public TextBlock() {}

    public TextBlock(String text, double x, double y, double width, double height, double confidence, float fontSize) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
        this.fontSize = fontSize;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
}
```

**Step 2: 更新 OcrService.java**
- 刪除 inner class `TextBlock`
- 所有 `OcrService.TextBlock` 的引用保持不變（import 會自動調整），或改為直接用 `TextBlock`

**Step 3: 全局搜尋替換**
搜尋所有檔案中的 `OcrService.TextBlock`，替換為 `TextBlock`：
- ProcessingService.java
- PdfService.java
- OfdService.java
- TesseractOcrService.java
- TextService.java

**Step 4: 驗證編譯**

```bash
cd /mnt/d/Projects/pdf_ofd_converter
mvn clean compile -q
```

Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: extract TextBlock to top-level class with proper encapsulation"
```

---

### Task 2: 抽取 OCR 引擎選擇邏輯為共用方法

**Objective:** 消除 ProcessingService.java 中重複 3 次的 OCR 引擎選擇 if-else 區塊

**Files:**
- Modify: `src/main/java/com/ocr/nospring/ProcessingService.java`

**Step 1: 在 ProcessingService 中新增共用方法**

在 ProcessingService.java 加入：

```java
/**
 * 執行 OCR 辨識，自動選擇引擎
 * @param image 來源圖片
 * @return OCR 辨識結果
 */
private List<TextBlock> runOcr(BufferedImage image) throws Exception {
    List<TextBlock> textBlocks;
    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
        if (tesseractService == null) {
            tesseractService = new TesseractOcrService(
                config.getTesseractDataPath(),
                TesseractLanguageHelper.getTesseractLanguage(language));
            log.info("  OCR Engine: Tesseract ({})",
                TesseractLanguageHelper.getTesseractLabel(language));
        }
        textBlocks = tesseractService.recognize(image);
    } else {
        textBlocks = ocrService.recognize(image, language);
        log.info("  OCR Engine: RapidOCR");
    }
    return textBlocks;
}
```

**Step 2: 替換 processMultiPage 中的重複區塊（約 line 102-111）**

替換：
```java
// BEFORE
List<OcrService.TextBlock> textBlocks;
if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
    if (tesseractService == null) { ... }
    textBlocks = tesseractService.recognize(image);
} else {
    textBlocks = ocrService.recognize(image, language);
}
```

為：
```java
// AFTER
List<TextBlock> textBlocks = runOcr(image);
```

**Step 3: 替換 processPerPage 中的重複區塊（約 line 217-226）**
同上替換。

**Step 4: 替換 processPdfToSearchable 中的重複區塊（約 line 327-338）**
同上替換。

**Step 5: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 6: Commit**

```bash
git add src/main/java/com/ocr/nospring/ProcessingService.java
git commit -m "refactor: extract OCR engine selection into shared runOcr() method"
```

---

### Task 3: 抽取文字轉換邏輯為共用方法

**Objective:** 消除 ProcessingService 中重複 3 次的文字轉換 if-else 區塊

**Files:**
- Modify: `src/main/java/com/ocr/nospring/ProcessingService.java`

**Step 1: 在 ProcessingService 中新增共用方法**

```java
/**
 * 執行文字簡繁轉換（如果設定中有指定）
 */
private void applyTextConversion(List<TextBlock> textBlocks) {
    if (config.getTextConvert() != null && !config.getTextConvert().isEmpty()) {
        convertTextBlocks(textBlocks, config.getTextConvert());
        log.info("  OK: Text converted ({})", config.getTextConvert());
    }
}
```

**Step 2: 替換三處重複的轉換區塊**

在 processMultiPage (line 114-116)、processPerPage (line 229-231)、processPdfToSearchable (line 341-342) 替換為：
```java
applyTextConversion(textBlocks);
```

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/ProcessingService.java
git commit -m "refactor: extract text conversion into shared applyTextConversion() method"
```

---

### Task 4: 抽取 OfdService 文字層渲染為共用方法

**Objective:** 消除 OfdService 中 generateOfd 和 generateMultiPageOfd 重複的 ~80 行逐字符定位邏輯

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OfdService.java`

**Step 1: 新增共用的文字層渲染方法**

```java
/**
 * 在虛擬頁面上繪製不可見文字層（逐字符絕對定位 + AWT 字體寬度計算）
 */
private void drawTextLayer(VirtualPage vPage, List<TextBlock> textBlocks,
                           double pageWidthMm, double pageHeightMm, int pageIndex) {
    for (TextBlock block : textBlocks) {
        try {
            String text = block.getText().trim();
            if (text == null || text.isEmpty()) continue;

            // 2. OCR 邊界框
            double ocrX = block.getX() * 25.4 / 72.0;
            double ocrY = block.getY() * 25.4 / 72.0;
            double ocrW = block.getWidth() * 25.4 / 72.0;
            double ocrH = block.getHeight() * 25.4 / 72.0;

            // 3. 字號保持 0.75 完美比例
            double fontSizeMm = ocrH * 0.75;
            float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);

            // 4. 使用 SERIF 字體（暫時維持，Task 5 會改用 config 字型）
            java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
                .deriveFont(fontSizePt);
            java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);

            // 5. Y 軸使用精確公式
            double ascentPt = awtFont.getLineMetrics(text, frc).getAscent();
            double ascentMm = ascentPt * 25.4 / 72.0;
            double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);

            // 6. 逐字符絕對定位
            double[] charWidthsMm = new double[text.length()];
            double totalAwtWidthMm = 0;

            for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                String singleChar = String.valueOf(text.charAt(charIdx));
                double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
                if (singleChar.equals(" ") && wPt == 0) {
                    wPt = fontSizePt * 0.3;
                }
                double wMm = wPt * 25.4 / 72.0;
                charWidthsMm[charIdx] = wMm;
                totalAwtWidthMm += wMm;
            }

            // 7. 計算縮放比例
            double scaleX = 1.0;
            if (totalAwtWidthMm > 0) {
                scaleX = ocrW / totalAwtWidthMm;
            }

            // 8. 逐字符繪製
            double currentX = ocrX;
            for (int charIdx = 0; charIdx < text.length(); charIdx++) {
                String singleChar = String.valueOf(text.charAt(charIdx));

                Span span = new Span(singleChar);
                span.setFontSize(fontSizeMm);
                span.setColor(config.getTextLayerRed(), config.getTextLayerGreen(), config.getTextLayerBlue());

                Paragraph p = new Paragraph();
                p.add(span);
                p.setPosition(Position.Absolute);
                p.setMargin(0d);
                p.setPadding(0d);
                p.setLineSpace(0d);
                p.setWidth(charWidthsMm[charIdx] * scaleX + 10.0);
                p.setX(currentX);
                p.setY(paragraphY);
                p.setOpacity(config.getTextLayerOpacity());
                vPage.add(p);

                currentX += (charWidthsMm[charIdx] * scaleX);
            }

        } catch (Exception e) {
            log.error("    Page {} - Error drawing text: {}", pageIndex + 1, e.getMessage());
        }
    }
}
```

**Step 2: 替換 generateMultiPageOfd 中的文字層區塊（line 80-164）**

替換整個 for 迴圈為：
```java
drawTextLayer(vPage, textBlocks, widthMm, heightMm, pageIndex);
```

**Step 3: 替換 generateOfd 中的文字層區塊（line 215-299）**

替換整個 for 迴圈為：
```java
drawTextLayer(vPage, textBlocks, widthMm, heightMm, 0);
```

**Step 4: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 5: Commit**

```bash
git add src/main/java/com/ocr/nospring/OfdService.java
git commit -m "refactor: extract OFD text layer rendering into shared drawTextLayer() method"
```

---

### Task 5: 抽取 OFD 頁面建立邏輯（消除坐標轉換重複）

**Objective:** 進一步消除 OfdService 中 generateOfd 和 generateMultiPageOfd 的頁面建立重複

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OfdService.java`

**Step 1: 新增共用的頁面建立方法**

```java
/**
 * 建立包含背景圖片和文字層的虛擬頁面
 */
private VirtualPage buildPage(Path tempImage, List<TextBlock> textBlocks,
                               double widthMm, double heightMm, int pageIndex) {
    PageLayout pageLayout = new PageLayout(widthMm, heightMm);
    pageLayout.setMargin(0d);
    VirtualPage vPage = new VirtualPage(pageLayout);

    // 添加背景圖片
    Img img = new Img(tempImage);
    img.setPosition(Position.Absolute)
       .setX(0d)
       .setY(0d)
       .setWidth(widthMm)
       .setHeight(heightMm);
    vPage.add(img);

    // 添加文字層
    drawTextLayer(vPage, textBlocks, widthMm, heightMm, pageIndex);

    return vPage;
}
```

**Step 2: 簡化 generateMultiPageOfd 和 generateOfd**

用 `buildPage()` 替換兩處的頁面建立邏輯。

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/OfdService.java
git commit -m "refactor: extract OFD page building into shared buildPage() method"
```

---

### Task 6: 抽取 TextService 重複的文字寫入邏輯

**Objective:** 消除 generateMultiPageTxt 和 generateTxt 中的重複寫入迴圈

**Files:**
- Modify: `src/main/java/com/ocr/nospring/TextService.java`

**Step 1: 新增共用的文字寫入方法**

```java
private void writeTextBlocks(BufferedWriter writer, List<TextBlock> textBlocks) throws java.io.IOException {
    for (TextBlock block : textBlocks) {
        String text = block.getText();
        if (text != null && !text.trim().isEmpty()) {
            writer.write(text);
            writer.newLine();
        }
    }
}
```

**Step 2: 簡化兩個 generate 方法**

在 `generateMultiPageTxt` 和 `generateTxt` 中用 `writeTextBlocks(writer, textBlocks)` 替換內部迴圈。

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/TextService.java
git commit -m "refactor: extract shared text writing method in TextService"
```

---

## Phase 2: 執行緒安全修復

### Task 7: 修復 OcrService 的 race condition

**Objective:** 修正 `initialized` flag 缺少 volatile 導致的 race condition

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OcrService.java`

**Step 1: 將 initialized 改為 volatile**

```java
// BEFORE
private boolean initialized = false;

// AFTER
private volatile boolean initialized = false;
```

**Step 2: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/ocr/nospring/OcrService.java
git commit -m "fix: add volatile to OcrService.initialized to prevent race condition"
```

---

### Task 8: 修復 ProcessingService 中 tesseractService 的 thread-safe lazy init

**Objective:** 確保 tesseractService 的延遲初始化是執行緒安全的

**Files:**
- Modify: `src/main/java/com/ocr/nospring/ProcessingService.java`

**Step 1: 在 runOcr() 方法中使用 synchronized**

```java
private synchronized List<TextBlock> runOcr(BufferedImage image) throws Exception {
    List<TextBlock> textBlocks;
    if (TesseractLanguageHelper.shouldUseTesseract(ocrEngine, language)) {
        if (tesseractService == null) {
            tesseractService = new TesseractOcrService(
                config.getTesseractDataPath(),
                TesseractLanguageHelper.getTesseractLanguage(language));
            log.info("  OCR Engine: Tesseract ({})",
                TesseractLanguageHelper.getTesseractLabel(language));
        }
        textBlocks = tesseractService.recognize(image);
    } else {
        textBlocks = ocrService.recognize(image, language);
        log.info("  OCR Engine: RapidOCR");
    }
    return textBlocks;
}
```

**Step 2: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/ocr/nospring/ProcessingService.java
git commit -m "fix: synchronize tesseractService lazy initialization in ProcessingService"
```

---

## Phase 3: 效能改善

### Task 9: OcrService 改用記憶體串流取代暫存檔

**Objective:** 消除每次 OCR 辨識都要寫暫存 PNG 到磁碟的效能瓶頸

**注意：** 需要先確認 RapidOCR 的 InferenceEngine 是否支援直接從 BufferedImage 或 byte[] 辨識。如果不支援，則改用 ByteArrayOutputStream 寫入記憶體而非檔案系統。

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OcrService.java`

**Step 1: 檢查 RapidOCR API**

查看 `InferenceEngine.runOcr()` 的方法簽名，確認是否接受 byte[] 或 InputStream。

**Step 2a: 如果 API 支援 byte[]/InputStream**

```java
public List<TextBlock> recognize(BufferedImage image, String language) throws Exception {
    if (!initialized) {
        initialize();
    }

    // 使用記憶體串流取代暫存檔
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "PNG", baos);
    byte[] imageBytes = baos.toByteArray();

    com.benjaminwan.ocrlibrary.OcrResult rapidResult =
        engine.runOcr(new ByteArrayInputStream(imageBytes));
    // ... rest of processing (unchanged)
}
```

**Step 2b: 如果 API 只接受檔案路徑**

保留暫存檔方案，但加入一個警告 log 標記此為已知瓶頸：

```java
log.debug("  Writing temp file for OCR engine (API limitation)");
```

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/OcrService.java
git commit -m "perf: eliminate temp file I/O in OcrService by using in-memory stream"
```

---

### Task 10: ProcessingService 的 SimpleDateFormat 改用 DateTimeFormatter

**Objective:** 用 Java 8+ 的 DateTimeFormatter 替代 thread-unsafe 的 SimpleDateFormat

**Files:**
- Modify: `src/main/java/com/ocr/nospring/ProcessingService.java`

**Step 1: 新增 class-level constant**

```java
private static final java.time.format.DateTimeFormatter TIMESTAMP_FORMATTER =
    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
```

**Step 2: 替換三處 SimpleDateFormat**

搜尋並替換：
```java
// BEFORE (出現 3 次)
String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

// AFTER
String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
```

**Step 3: 移除不再需要的 Date import（如果有的話）**

**Step 4: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 5: Commit**

```bash
git add src/main/java/com/ocr/nospring/ProcessingService.java
git commit -m "refactor: replace SimpleDateFormat with DateTimeFormatter in ProcessingService"
```

---

## Phase 4: 編碼與穩定性

### Task 11: TextService 加入 UTF-8 編碼指定

**Objective:** 確保 TXT 輸出檔案永遠使用 UTF-8，避免平台預設編碼造成的亂碼

**Files:**
- Modify: `src/main/java/com/ocr/nospring/TextService.java`

**Step 1: 新增 import**

```java
import java.nio.charset.StandardCharsets;
import java.io.OutputStreamWriter;
```

**Step 2: 替換 FileWriter 為 OutputStreamWriter**

```java
// BEFORE
new BufferedWriter(new FileWriter(outputFile))

// AFTER (出現 2 次)
new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))
```

**Step 3: 新增 import**

```java
import java.io.FileOutputStream;
```

**Step 4: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 5: Commit**

```bash
git add src/main/java/com/ocr/nospring/TextService.java
git commit -m "fix: enforce UTF-8 encoding in TextService file output"
```

---

### Task 12: OfdService 使用 Config 的字型設定

**Objective:** 修正 OfdService 永遠使用 Font.SERIF 而忽略 Config 字型設定的 bug

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OfdService.java`

**Step 1: 新增字型載入方法**

```java
private java.awt.Font loadAwtFont(float fontSizePt) {
    // 優先使用 config 設定的字型
    if (config.getFontPath() != null && !config.getFontPath().isEmpty()) {
        try {
            java.awt.Font customFont = java.awt.Font.createFont(
                java.awt.Font.TRUETYPE_FONT, new File(config.getFontPath()));
            return customFont.deriveFont(fontSizePt);
        } catch (Exception e) {
            log.warn("  Failed to load configured font: {}, falling back to SERIF", config.getFontPath());
        }
    }
    // Fallback: 系統 SERIF
    return new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1).deriveFont(fontSizePt);
}
```

**Step 2: 替換 drawTextLayer 中的字型建立**

```java
// BEFORE
java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
    .deriveFont(fontSizePt);

// AFTER
java.awt.Font awtFont = loadAwtFont(fontSizePt);
```

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/OfdService.java
git commit -m "fix: use configured font in OfdService instead of hardcoded Font.SERIF"
```

---

### Task 13: OcrService 使用真實的 OCR confidence

**Objective:** 使用 OCR 引擎回傳的真實 confidence 值，而非硬編碼的 0.9

**Files:**
- Modify: `src/main/java/com/ocr/nospring/OcrService.java`

**Step 1: 檢查 RapidOCR TextBlock API**

確認 `com.benjaminwan.ocrlibrary.TextBlock` 是否有 getConfidence() 或類似方法。

**Step 2: 替換硬編碼 confidence**

```java
// BEFORE
tb.confidence = 0.9; // Default confidence

// AFTER
tb.setConfidence(block.getConfidence()); // 使用 OCR 引擎的真實 confidence
```

如果 RapidOCR TextBlock 沒有 confidence 欄位，保持 0.9 但加上明確的 TODO 註解：
```java
// TODO: RapidOCR TextBlock does not expose per-block confidence; using placeholder
tb.setConfidence(0.9);
```

**Step 3: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 4: Commit**

```bash
git add src/main/java/com/ocr/nospring/OcrService.java
git commit -m "fix: use real OCR confidence from RapidOCR engine instead of hardcoded 0.9"
```

---

## Phase 5: TesseractLanguageHelper 重構

### Task 14: 用 Map 取代 20+ 個 isXxx() 方法

**Objective:** 將 TesseractLanguageHelper 從大量重複的 boolean 方法重構為 Map-driven 架構，提升可維護性

**Files:**
- Modify: `src/main/java/com/ocr/nospring/TesseractLanguageHelper.java`

**Step 1: 建立語言資料結構**

```java
private record LanguageMapping(
    String tessLang,       // Tesseract 語言代碼
    String displayName     // 顯示名稱
) {}

private static final Map<String, LanguageMapping> TESSERACT_LANGUAGES = Map.ofEntries(
    Map.entry("hebrew", new LanguageMapping("heb+eng", "Hebrew")),
    Map.entry("he", new LanguageMapping("heb+eng", "Hebrew")),
    Map.entry("thai", new LanguageMapping("tha+eng", "Thai")),
    Map.entry("th", new LanguageMapping("tha+eng", "Thai")),
    Map.entry("russian", new LanguageMapping("rus+eng", "Russian")),
    Map.entry("ru", new LanguageMapping("rus+eng", "Russian")),
    Map.entry("arabic", new LanguageMapping("ara+eng", "Arabic")),
    Map.entry("ar", new LanguageMapping("ara+eng", "Arabic")),
    Map.entry("korean", new LanguageMapping("kor+eng", "Korean")),
    Map.entry("ko", new LanguageMapping("kor+eng", "Korean")),
    Map.entry("japanese", new LanguageMapping("jpn+eng", "Japanese")),
    Map.entry("ja", new LanguageMapping("jpn+eng", "Japanese")),
    Map.entry("french", new LanguageMapping("fra+eng", "French")),
    Map.entry("fr", new LanguageMapping("fra+eng", "French")),
    Map.entry("german", new LanguageMapping("deu+eng", "German")),
    Map.entry("de", new LanguageMapping("deu+eng", "German")),
    Map.entry("spanish", new LanguageMapping("spa+eng", "Spanish")),
    Map.entry("es", new LanguageMapping("spa+eng", "Spanish")),
    Map.entry("greek", new LanguageMapping("ell+eng", "Greek")),
    Map.entry("el", new LanguageMapping("ell+eng", "Greek")),
    Map.entry("hindi", new LanguageMapping("hin+eng", "Hindi")),
    Map.entry("hi", new LanguageMapping("hin+eng", "Hindi")),
    Map.entry("persian", new LanguageMapping("fas+eng", "Persian")),
    Map.entry("fa", new LanguageMapping("fas+eng", "Persian")),
    Map.entry("vietnamese", new LanguageMapping("vie+eng", "Vietnamese")),
    Map.entry("vi", new LanguageMapping("vie+eng", "Vietnamese")),
    Map.entry("italian", new LanguageMapping("ita+eng", "Italian")),
    Map.entry("it", new LanguageMapping("ita+eng", "Italian")),
    Map.entry("portuguese", new LanguageMapping("por+eng", "Portuguese")),
    Map.entry("pt", new LanguageMapping("por+eng", "Portuguese"))
);
```

**Step 2: 重寫 shouldUseTesseract、getTesseractLanguage、getTesseractLabel**

```java
public static boolean shouldUseTesseract(String engine, String language) {
    if ("tesseract".equalsIgnoreCase(engine)) return true;
    if ("rapidocr".equalsIgnoreCase(engine)) return false;
    // auto mode: 如果語言有 Tesseract mapping，且不在中英文列表中
    return !isChineseOrEnglish(language) && TESSERACT_LANGUAGES.containsKey(normalizeLanguageKey(language));
}

public static String getTesseractLanguage(String language) {
    LanguageMapping mapping = TESSERACT_LANGUAGES.get(normalizeLanguageKey(language));
    return mapping != null ? mapping.tessLang() : "eng";
}

public static String getTesseractLabel(String language) {
    LanguageMapping mapping = TESSERACT_LANGUAGES.get(normalizeLanguageKey(language));
    return mapping != null ? mapping.displayName() : language;
}

private static String normalizeLanguageKey(String language) {
    if (language == null) return "";
    String key = language.toLowerCase().trim();
    // 處理 chi_sim, chi_tra 的特殊情況
    if (key.startsWith("chi_")) return key;
    // 取第一個部分（處理 "chinese_cht" 之類的）
    int underscoreIdx = key.indexOf('_');
    if (underscoreIdx > 0) {
        String prefix = key.substring(0, underscoreIdx);
        // 檢查是否有對應的 mapping
        if (TESSERACT_LANGUAGES.containsKey(prefix)) return prefix;
    }
    return key;
}
```

**Step 3: 保留 isChineseOrEnglish 方法（邏輯獨立，不需要 map 化）**

**Step 4: 驗證編譯**

```bash
mvn clean compile -q
```

**Step 5: Commit**

```bash
git add src/main/java/com/ocr/nospring/TesseractLanguageHelper.java
git commit -m "refactor: replace 20+ isXxx() methods with Map-driven language mapping"
```

---

## Phase 6: 清理與整理

### Task 15: 統一版本號管理

**Objective:** 統一 Maven 版本、conveyor.conf 版本、和 CHANGELOG 版本號

**Files:**
- Modify: `pom.xml` (version 欄位)
- Modify: `conveyor.conf` (app-version 欄位)
- Verify: `CHANGELOG.md`

**Step 1: 確認目前版本**

檢查 pom.xml、conveyor.conf、CHANGELOG.md 的版本號，決定統一版本。

**Step 2: 統一版本號**

確保 pom.xml `<version>` 和 conveyor.conf `app-version` 一致。

**Step 3: 更新 repack 腳本中的版本號**

修正 `scripts/repack-into-zip.ps1` 中的硬編碼版本號。

**Step 4: Commit**

```bash
git add pom.xml conveyor.conf scripts/repack-into-zip.ps1
git commit -m "chore: unify version numbers across pom.xml, conveyor.conf, and build scripts"
```

---

### Task 16: 清理 stale 檔案和更新 .gitignore

**Objective:** 移除過時的 jpackage 腳本和根目錄雜物

**Files:**
- Delete: `scripts/build-exe.ps1`, `scripts/build.ps1`, `scripts/build-jpackage.ps1`
- Modify: `.gitignore` (新增條目)

**Step 1: 更新 .gitignore**

```gitignore
# IDE
.classpath
.project
.settings/

# Build artifacts
dependency-reduced-pom.xml

# Unrelated files
entities.json
mempalace.yaml

# Legacy build output
dist-exe/

# AI agent context
.claude/
```

**Step 2: 刪除 stale 腳本**

```bash
git rm scripts/build-exe.ps1 scripts/build.ps1 scripts/build-jpackage.ps1
```

**Step 3: 移除已追蹤但應忽略的檔案**

```bash
git rm --cached .classpath .project dependency-reduced-pom.xml entities.json mempalace.yaml 2>/dev/null
```

**Step 4: Commit**

```bash
git add .gitignore
git commit -m "chore: clean up stale scripts and update .gitignore"
```

---

## 完成後驗證

### Final Verification

```bash
# 完整編譯 + 打包
mvn clean package -q

# 確認所有測試通過（目前無測試，此步驟未來需補上）
# mvn test

# 確認分支乾淨
git status
git log --oneline -20
```

---

## Commit 歷史預覽

```
refactor: extract TextBlock to top-level class with proper encapsulation
refactor: extract OCR engine selection into shared runOcr() method
refactor: extract text conversion into shared applyTextConversion() method
refactor: extract OFD text layer rendering into shared drawTextLayer() method
refactor: extract OFD page building into shared buildPage() method
refactor: extract shared text writing method in TextService
fix: add volatile to OcrService.initialized to prevent race condition
fix: synchronize tesseractService lazy initialization in ProcessingService
perf: eliminate temp file I/O in OcrService by using in-memory stream
refactor: replace SimpleDateFormat with DateTimeFormatter in ProcessingService
fix: enforce UTF-8 encoding in TextService file output
fix: use configured font in OfdService instead of hardcoded Font.SERIF
fix: use real OCR confidence from RapidOCR engine instead of hardcoded 0.9
refactor: replace 20+ isXxx() methods with Map-driven language mapping
chore: unify version numbers across pom.xml, conveyor.conf, and build scripts
chore: clean up stale scripts and update .gitignore
```

**預計影響：**
- 16 commits，每個都是原子性的單一改動
- 零功能變更，純重構和 bug fix
- 降低 ~40% 的重複程式碼
- 修復 3 個執行緒安全問題
- 修正 2 個 bug（OFD 字型、UTF-8 編碼）
- 1 個效能改善（暫存檔消除）
