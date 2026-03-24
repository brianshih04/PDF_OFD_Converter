# Searchable PDF/OFD 產生方法

## 概述

本文檔說明如何生成可搜索的 PDF 和 OFD 文件（Searchable PDF/OFD），包含透明文字層的技術實現細節。

---

## 核心原理

### 雙層結構

Searchable PDF/OFD 由兩層組成：

```
┌─────────────────────────────┐
│   透明文字層（可搜索）        │  ← OCR 識別的文字
├─────────────────────────────┤
│   背景圖片層（原始影像）      │  ← 掃描的 JPG/PNG
└─────────────────────────────┘
```

**關鍵點**：
- 背景層：原始掃描圖片
- 文字層：透明顯示但可被搜索和複製
- 文字層位置必須與圖片中的文字精確對齊

---

## 技術實現

### 1. OCR 識別

使用 **RapidOcrOnnxLibrary** 進行 OCR 識別：

```java
OcrService ocrService = new OcrService();
List<OcrService.TextBlock> textBlocks = ocrService.recognize(image, "chinese_cht");
```

每個 `TextBlock` 包含：
- `text`: 識別的文字
- `x, y`: 左上角坐標（像素）
- `width, height`: 邊界框大小（像素）

### 2. 坐標轉換

將像素坐標轉換為毫米（假設 DPI = 72）：

```java
double ocrX = block.x * 25.4 / 72.0;
double ocrY = block.y * 25.4 / 72.0;
double ocrW = block.width * 25.4 / 72.0;
double ocrH = block.height * 25.4 / 72.0;
```

### 3. Y 軸定位公式

**最終公式**（經多次測試優化）：

```java
double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
```

**調整歷程**：
| 偏移量 | 結果 |
|--------|------|
| 無偏移 | 太低 |
| 往上 1/3 字高 | 太高 |
| 往上 1/6 字高 | 仍然太高 |
| **往上 0.1 字高** | ✅ 最佳 |

### 4. 逐字符絕對定位算法

**為什麼需要逐字符定位？**
- OCR 邊界框是整段文字的邊界
- 不同字符寬度不同
- 直接使用整段定位會導致字符間距不準確

**算法步驟**：

```java
// 1. 使用 AWT 計算每個字符的實際寬度
java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
    .deriveFont(fontSizePt);
java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);

// 2. 計算每個字符寬度
double[] charWidthsMm = new double[text.length()];
double totalAwtWidthMm = 0;

for (int charIdx = 0; charIdx < text.length(); charIdx++) {
    String singleChar = String.valueOf(text.charAt(charIdx));
    double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
    
    // 處理空白字符
    if (singleChar.equals(" ") && wPt == 0) {
        wPt = fontSizePt * 0.3;
    }
    
    double wMm = wPt * 25.4 / 72.0;
    charWidthsMm[charIdx] = wMm;
    totalAwtWidthMm += wMm;
}

// 3. 計算縮放比例（使文字填滿 OCR 邊界框）
double scaleX = ocrW / totalAwtWidthMm;

// 4. 逐字符繪製
double currentX = ocrX;

for (int charIdx = 0; charIdx < text.length(); charIdx++) {
    String singleChar = String.valueOf(text.charAt(charIdx));
    
    // 創建文字元素
    Span span = new Span(singleChar);
    span.setFontSize(fontSizeMm);
    span.setColor(red, green, blue);
    
    Paragraph p = new Paragraph();
    p.add(span);
    p.setX(currentX);
    p.setY(paragraphY);
    p.setOpacity(opacity);
    
    // 添加到頁面
    vPage.add(p);
    
    // 坐標推進
    currentX += (charWidthsMm[charIdx] * scaleX);
}
```

### 5. 文字層透明度設置

**生產環境**（可搜索但看不見）：
```java
span.setColor(255, 255, 255);  // 白色
p.setOpacity(0.0001);           // 極低透明度
```

**調試環境**（可看見文字定位）：
```java
span.setColor(255, 0, 0);      // 紅色
p.setOpacity(1.0);              // 不透明
```

**重要**：
- 透明度不能設為 0.0，否則某些閱讀器無法搜索
- 建議使用 0.0001（極低透明度）

---

## JSON 配置

### 配置結構

```json
{
  "input": {
    "folder": "C:/OCR/Input",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/OCR/Output",
    "format": "ofd"
  },
  "ocr": {
    "language": "chinese_cht"
  },
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

### textLayer 配置選項

| 參數 | 類型 | 默認值 | 說明 |
|------|------|--------|------|
| `color` | String | `"white"` | 文字層顏色名稱 |
| `red` | Integer | `255` | RGB 紅色值 (0-255) |
| `green` | Integer | `255` | RGB 綠色值 (0-255) |
| `blue` | Integer | `255` | RGB 藍色值 (0-255) |
| `opacity` | Double | `0.0001` | 透明度 (0.0 - 1.0) |

### 支持的顏色名稱

| 顏色 | RGB | 用途 |
|------|-----|------|
| `"white"` | (255, 255, 255) | 生產環境（默認） |
| `"red"` | (255, 0, 0) | 調試 |
| `"black"` | (0, 0, 0) | 特殊用途 |
| `"blue"` | (0, 0, 255) | 特殊用途 |
| `"green"` | (0, 255, 0) | 特殊用途 |
| `"debug"` | (255, 0, 0) + opacity=1.0 | **調試模式** |

### 配置示例

#### 生產環境配置
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

#### 調試環境配置
```json
{
  "textLayer": {
    "color": "debug"
  }
}
```

#### 自定義 RGB
```json
{
  "textLayer": {
    "red": 128,
    "green": 128,
    "blue": 128,
    "opacity": 0.5
  }
}
```

---

## 使用方法

### 命令行

```powershell
# 生產模式
java -Xmx2G -jar jpeg2pdf-ofd-nospring.jar config.json

# 調試模式（可看見文字定位）
java -Xmx2G -jar jpeg2pdf-ofd-nospring.jar config-debug.json
```

### 測試步驟

1. **生成調試版本**
   ```json
   {
     "textLayer": { "color": "debug" }
   }
   ```
   用 WPS 打開，觀察紅色文字是否與圖片中的文字對齊

2. **調整偏移量**（如需要）
   - 修改 `OfdService.java` 中的 Y 軸公式
   - 重新編譯：`mvn clean package`

3. **生成生產版本**
   ```json
   {
     "textLayer": { "color": "white", "opacity": 0.0001 }
   }
   ```

4. **驗證搜索功能**
   - 用 WPS 打開 OFD
   - 按 Ctrl+F 搜索關鍵字
   - 確認搜索結果正確高亮

---

## 常見問題

### Q1: WPS 無法搜索文字？

**檢查清單**：
- ✅ 透明度 > 0（不能為 0.0）
- ✅ 文字層已生成（檢查文件大小是否增加）
- ✅ OCR 識別成功（檢查日誌中的 "OCR completed"）
- ✅ 使用支持的閱讀器（WPS Office、福昕閱讀器）

### Q2: 文字定位不準確？

**解決方案**：
1. 使用 `"color": "debug"` 生成調試版本
2. 觀察紅色文字與圖片的偏差
3. 調整 Y 軸偏移量
4. 使用逐字符定位（已實現）

### Q3: 字體加載錯誤？

**解決方案**：
- 優先使用 `.ttf` 文件（如 `simhei.ttf`）
- 避免使用 `.ttc` 文件（字體集合）
- 檢查字體文件路徑是否正確

### Q4: 生成的文件太大？

**原因**：
- 每個字符都是獨立的文字元素
- 大量文字會增加文件大小

**優化建議**：
- 減少 DPI（但會降低質量）
- 壓縮圖片
- 使用 PDF 而非 OFD（文件較小）

---

## 技術細節

### DPI 假設

本實現假設圖片 DPI = 72。如果實際 DPI 不同，需要調整轉換公式：

```java
// 當前實現（假設 72 DPI）
double mm = pixel * 25.4 / 72.0;

// 如果 DPI 不是 72
double mm = pixel * 25.4 / actualDPI;
```

### 字體選擇

使用 `java.awt.Font.SERIF` 作為默認字體，原因：
- 系統內置，無需外部文件
- 支持多種語言
- 渲染穩定

### 支持的 OCR 語言

- `chinese_cht` - 繁體中文
- `chinese_sim` - 簡體中文
- `english` - 英文
- `japanese` - 日文
- 其他 80+ 種語言

---

## 相關文件

- **技術筆記**: `SEARCHABLE_OFD_NOTES.md`
- **JSON 配置指南**: `JSON-CONFIG-GUIDE.md`
- **原始碼**:
  - `src/main/java/com/ocr/nospring/OfdService.java` - OFD 生成
  - `src/main/java/com/ocr/nospring/PdfService.java` - PDF 生成
  - `src/main/java/com/ocr/nospring/Config.java` - 配置類

---

**最後更新**: 2026-03-24
**GitHub**: https://github.com/brianshih04/jpeg2pdf-ofd-jpackage-test
