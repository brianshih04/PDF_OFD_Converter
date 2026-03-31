# Searchable OFD 技術筆記

## 概述
記錄如何生成可搜索的 OFD 文件（Searchable OFD），包含透明文字層的技術細節。

## 核心技術

### 1. 雙層結構
- **背景層**: 原始圖片（JPG/PNG）
- **文字層**: OCR 識別的文字，透明顯示但可搜索

### 2. 透明文字層實現

#### 關鍵參數（最終優化版本）
```java
// 文字顏色
span.setColor(255, 255, 255); // 白色

// 透明度
p.setOpacity(0.0001); // 極低透明度，幾乎看不見但可搜索

// Y 軸偏移
double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
```

#### 調試技巧
- 開發時將透明度設為 `1.0`，顏色設為紅色 `(255, 0, 0)`，方便觀察文字定位
- 完成後改為白色 + 極低透明度

### 3. 精確定位算法

#### 逐字符絕對定位（終極方案）
```java
// 1. 使用 AWT 字體計算每個字符的實際寬度
java.awt.Font awtFont = new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, 1)
    .deriveFont(fontSizePt);
java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);

// 2. 計算每個字符的寬度
double[] charWidthsMm = new double[text.length()];
for (int charIdx = 0; charIdx < text.length(); charIdx++) {
    String singleChar = String.valueOf(text.charAt(charIdx));
    double wPt = awtFont.getStringBounds(singleChar, frc).getWidth();
    charWidthsMm[charIdx] = wPt * 25.4 / 72.0;
}

// 3. 計算縮放比例（使文字填滿 OCR 識別的邊界框）
double scaleX = ocrW / totalAwtWidthMm;

// 4. 逐字符繪製，每個字符獨立定位
for (int charIdx = 0; charIdx < text.length(); charIdx++) {
    Span span = new Span(singleChar);
    Paragraph p = new Paragraph();
    p.add(span);
    p.setX(currentX);
    p.setY(paragraphY);
    vPage.add(p);
    currentX += (charWidthsMm[charIdx] * scaleX);
}
```

### 4. Y 軸定位公式

#### 最終公式
```java
double paragraphY = (ocrY + (ocrH * 0.72)) - ascentMm - (ocrH * 0.1);
```

#### 調整歷程
- 初始版本：無偏移
- 第一次調整：往上 1/3 字高 → 太高
- 第二次調整：往上 1/6 字高 → 仍然太高
- **最終版本：往上 0.1 字高** → ✅ 最佳位置

### 5. 坐標轉換

#### 像素轉毫米（假設 DPI = 72）
```java
double widthMm = image.getWidth() * 25.4 / 72.0;
double heightMm = image.getHeight() * 25.4 / 72.0;
double ocrX = block.x * 25.4 / 72.0;
double ocrY = block.y * 25.4 / 72.0;
```

### 6. 字號計算
```java
double fontSizeMm = ocrH * 0.75; // OCR 高度的 75%
float fontSizePt = (float) (fontSizeMm * 72.0 / 25.4);
```

## 使用工具

### OCR 引擎
- **RapidOcrOnnxLibrary**: 支持中英文識別
- 模型：ch_PP-OCRv4

### OFD 庫
- **ofdrw-layout**: OFD 文檔生成
- 核心 API：
  - `OFDDoc`: 文檔對象
  - `VirtualPage`: 虛擬頁面
  - `Span`: 文字片段
  - `Paragraph`: 段落容器
  - `Img`: 圖片對象

## JSON 配置

### textLayer 配置（文字層設置）

```json
{
  "textLayer": {
    "color": "white",    // 顏色名稱：white/red/black/blue/green/debug
    "opacity": 0.0001    // 透明度 (0.0 - 1.0)
  }
}
```

### 支持的顏色名稱
- `"white"` - 白色（默認，生產環境）
- `"red"` - 紅色
- `"black"` - 黑色
- `"blue"` - 藍色
- `"green"` - 綠色
- `"debug"` - **調試模式**：紅色 + 不透明（opacity=1.0）

### 自定義 RGB
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

### 調試配置示例
```json
{
  "textLayer": {
    "color": "debug"    // 紅色、不透明，方便觀察文字定位
  }
}
```

### 生產配置示例
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001   // 極低透明度，可搜索但幾乎看不見
  }
}
```

## 常見問題

### Q1: WPS 無法搜索文字？
**解決方案**:
1. 確保透明度 > 0（建議 0.0001）
2. 確保文字顏色為白色或其他淺色
3. 使用逐字符定位而非整段文字定位

### Q2: 文字定位不準確？
**解決方案**:
1. 使用 AWT 計算每個字符的實際寬度
2. 使用逐字符絕對定位
3. 調整 Y 軸偏移量

### Q3: 字體加載錯誤？
**解決方案**:
- 優先使用 `.ttf` 文件（如 `simhei.ttf`）
- 避免使用 `.ttc` 文件（字體集合）

## 參考文件
- 原始成功實現：`JPEG2PDF_OFD_Java_2/src/main/java/com/ocr/jpeg2pdf/service/impl/OfdLayoutDirectServiceImpl.java`
- 測試專案：`PDF_OFD_Converter`

## 測試方法
1. 用 WPS 打開生成的 OFD 文件
2. 按 Ctrl+F 搜索關鍵字
3. 確認搜索結果高亮顯示

---
**建立日期**: 2026-03-24
**最後更新**: 2026-03-24
