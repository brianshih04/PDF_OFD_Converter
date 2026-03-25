# JSON 配置文件完整指南

## 📋 目錄

- [完整配置結構](#完整配置結構)
- [input 配置](#input-配置)
- [output 配置](#output-配置)
- [ocr 配置](#ocr-配置)
- [textConvert 配置](#textconvert-配置簡繁轉換)
- [textLayer 配置](#textlayer-配置)
- [font 配置](#font-配置)
- [完整示例](#完整示例)
- [注意事項](#注意事項)

---

## 完整配置結構

```json
{
  "input": {
    "folder": "C:/OCR/Input",
    "pattern": "*.jpg",
    "extensions": ["jpg", "jpeg", "png"]
  },
  "output": {
    "folder": "C:/OCR/Output",
    "format": "all",
    "multiPage": false
  },
  "ocr": {
    "language": "chinese_cht",
    "useGpu": false,
    "cpuThreads": 4
  },
  "textConvert": "s2t",
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  },
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

---

## input 配置（輸入設置）

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `folder` | String | ✅ | - | 輸入圖片所在的資料夾路徑 |
| `file` | String | ⚪ | - | 單個圖片文件路徑（與 `folder` 二選一） |
| `pattern` | String | ⚪ | `*.jpg` | 文件過濾模式（支持 `*.jpg`, `*.png`, `*.*`） |
| `extensions` | Array | ⚪ | `["jpg", "jpeg", "png"]` | 支持的圖片擴展名列表 |

### 配置示例

#### 示例 1：處理整個資料夾

```json
{
  "input": {
    "folder": "C:/OCR/Watch",
    "pattern": "*.jpg"
  }
}
```

**說明：**
- 處理 `C:/OCR/Watch/` 資料夾下所有 `.jpg` 文件
- 不包括 `.jpeg`, `.png` 等其他格式

---

#### 示例 2：處理單個文件

```json
{
  "input": {
    "file": "C:/Documents/scan001.jpg"
  }
}
```

**說明：**
- 只處理指定的單個文件
- `file` 和 `folder` 不能同時使用

---

#### 示例 3：支持多種格式

```json
{
  "input": {
    "folder": "C:/OCR/Watch",
    "extensions": ["jpg", "jpeg", "png", "bmp", "gif"]
  }
}
```

**說明：**
- 處理多種圖片格式
- 包括 `.jpg`, `.jpeg`, `.png`, `.bmp`, `.gif`

---

#### 示例 4：處理所有文件

```json
{
  "input": {
    "folder": "C:/OCR/Watch",
    "pattern": "*.*"
  }
}
```

**說明：**
- 處理資料夾下所有文件
- 程序會自動過濾非圖片文件

---

## output 配置（輸出設置）

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `folder` | String | ✅ | - | 輸出文件存放的資料夾路徑 |
| `format` | String | ⚪ | `"pdf"` | 輸出格式（字串，推薦） |
| `formats` | Array | ⚪ | `["pdf"]` | 輸出格式列表（陣列，向下兼容） |
| `multiPage` | Boolean | ⚪ | `false` | 是否合併為多頁文檔 |

> `format` 和 `formats` 二選一即可。`format` 使用字串，`formats` 使用陣列，效果相同。

### format 可選值

| 值 | 說明 | 輸出文件 |
|----|------|---------|
| `"pdf"` | 只生成 PDF | `output.pdf` |
| `"ofd"` | 只生成 OFD（中國國家標準格式） | `output.ofd` |
| `"txt"` | 只生成 TXT（純文字） | `output.txt` |
| `"all"` | 生成所有格式 | `output.pdf`, `output.ofd`, `output.txt` |
| `"pdf,ofd"` | 生成 PDF + OFD | `output.pdf`, `output.ofd` |

### 配置示例

#### 示例 1：單頁模式（默認）

```json
{
  "output": {
    "folder": "C:/OCR/Output",
    "format": "all",
    "multiPage": false
  }
}
```

**輸出：**
```
image1_20260323_130000.pdf
image1_20260323_130000.ofd
image1_20260323_130000.txt

image2_20260323_130001.pdf
image2_20260323_130001.ofd
image2_20260323_130001.txt

image3_20260323_130002.pdf
image3_20260323_130002.ofd
image3_20260323_130002.txt
```

**適用場景：**
- 每個圖片需要單獨處理
- 靈活的文件管理
- 可以選擇性處理單個文件

---

#### 示例 2：多頁模式（合併所有圖片）

```json
{
  "output": {
    "folder": "C:/OCR/Output",
    "format": "all",
    "multiPage": true
  }
}
```

**輸出：**
```
multipage_20260323_130000.pdf  (包含所有頁面)
multipage_20260323_130000.ofd  (包含所有頁面)
multipage_20260323_130000.txt  (包含所有文字)
```

**適用場景：**
- 處理掃描文檔（多頁文檔）
- 批量報告生成
- 文檔歸檔

---

#### 示例 3：只生成 PDF

```json
{
  "output": {
    "folder": "C:/OCR/Output",
    "format": "pdf",
    "multiPage": false
  }
}
```

**說明：**
- 只生成 PDF 文件
- 不生成 OFD 和 TXT

---

#### 示例 4：PDF + OFD（無 TXT）

```json
{
  "output": {
    "folder": "C:/OCR/Output",
    "format": "pdf,ofd",
    "multiPage": true
  }
}
```

**說明：**
- 生成 PDF 和 OFD 兩種格式
- 不生成 TXT 文件

---

## ocr 配置（OCR 引擎設置）

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `language` | String | ⚪ | `chinese_cht` | OCR 識別語言 |
| `useGpu` | Boolean | ⚪ | `false` | 是否使用 GPU 加速 |
| `cpuThreads` | Integer | ⚪ | `4` | CPU 線程數 |

### 支持語言（80+ 種）

| 語言代碼 | 語言名稱 | 語言代碼 | 語言名稱 |
|---------|---------|---------|---------|
| `chinese_cht` | 繁體中文（默認） | `ch` | 簡體中文 |
| `en` | 英文 | `japan` | 日文 |
| `korean` | 韓文 | `french` | 法文 |
| `german` | 德文 | `spanish` | 西班牙文 |
| `portuguese` | 葡萄牙文 | `russian` | 俄文 |
| `arabic` | 阿拉伯文 | `hindi` | 印地文 |
| `thai` | 泰文 | `vietnamese` | 越南文 |
| `italian` | 意大利文 | `dutch` | 荷蘭文 |

**提示：** `chinese_cht` 可以識別繁體中文 + 英文混合文檔

### 配置示例

#### 示例 1：英文文檔

```json
{
  "ocr": {
    "language": "en"
  }
}
```

**適用場景：**
- 純英文文檔
- 英文表單、合同

---

#### 示例 2：簡體中文

```json
{
  "ocr": {
    "language": "ch"
  }
}
```

**適用場景：**
- 簡體中文文檔
- 中國大陸文件

---

#### 示例 3：日文文檔

```json
{
  "ocr": {
    "language": "japan",
    "cpuThreads": 6
  }
}
```

**適用場景：**
- 日文文檔
- 日本進口文件

---

#### 示例 4：高性能配置（有 GPU）

```json
{
  "ocr": {
    "language": "en",
    "useGpu": true,
    "cpuThreads": 8
  }
}
```

**適用場景：**
- 高性能機器（有 GPU）
- 大批量處理
- 需要快速處理

---

## textConvert 配置（簡繁轉換）

OCR 識別結果可能混合簡繁體中文。使用 OpenCC 可在生成 PDF/OFD 前自動轉換。

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `textConvert` | String | ⚪ | `null`（不轉換） | `"s2t"` 簡體→繁體，`"t2s"` 繁體→簡體 |

### 為什麼需要此功能？

即使 OCR 語言設為 `chinese_cht`（繁體中文），RapidOCR 的輸出仍可能混合簡體字：

| 原始圖片 | OCR 輸出 | 問題 |
|---------|---------|------|
| 資產負債表 | 資產負**价**表 | 「債」變成簡體「价」 |
| 帳戶 | **帐**戶 | 「帳」變成簡體「帐」 |
| 流動資產 | **流动**資產 | 「動」變成簡体「动」 |
| 金額 | 金**额** | 「額」變成簡体「额」 |

加上 `"textConvert": "s2t"` 後，所有文字都會轉換為正確的繁體。

### 配置示例

#### 示例 1：簡體→繁體（最常用）

```json
{
  "ocr": {
    "language": "chinese_cht"
  },
  "textConvert": "s2t"
}
```

**適用場景：**
- 台灣/香港的繁體中文文件
- OCR 輸出混合簡繁體

#### 示例 2：繁體→簡體

```json
{
  "ocr": {
    "language": "ch"
  },
  "textConvert": "t2s"
}
```

**適用場景：**
- 需要統一為簡體中文的文件

#### 示例 3：不轉換（默認）

```json
{
  "ocr": {
    "language": "chinese_cht"
  }
  // 不設定 textConvert，保持 OCR 原始輸出
}
```

**適用場景：**
- OCR 輸出已經正確
- 不需要簡繁轉換

---

## textLayer 配置（文字層設置）

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `color` | String | ⚪ | `"white"` | 文字層顏色（顏色名稱） |
| `red` | Integer | ⚪ | `255` | RGB 紅色值 (0-255) |
| `green` | Integer | ⚪ | `255` | RGB 綠色值 (0-255) |
| `blue` | Integer | ⚪ | `255` | RGB 藍色值 (0-255) |
| `opacity` | Double | ⚪ | `0.0001` | 透明度 (0.0 - 1.0) |

### 支持的顏色名稱

| 顏色名稱 | RGB 值 | 說明 |
|----------|--------|------|
| `"white"` | (255, 255, 255) | 白色（默認，推薦） |
| `"black"` | (0, 0, 0) | 黑色 |
| `"red"` | (255, 0, 0) | 紅色 |
| `"green"` | (0, 255, 0) | 綠色 |
| `"blue"` | (0, 0, 255) | 藍色 |
| `"debug"` | (255, 0, 0) + opacity=1.0 | 調試模式（紅色不透明） |

### 配置示例

#### 示例 1：默認設置（生產環境）

```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

**說明：**
- 白色文字，極低透明度
- 文字幾乎看不見，但可搜索
- **推薦用於生產環境**

---

#### 示例 2：調試模式

```json
{
  "textLayer": {
    "color": "debug"
  }
}
```

**說明：**
- 紅色文字，不透明（opacity = 1.0）
- 方便觀察文字定位是否準確
- **僅用於開發調試**

---

#### 示例 3：自定義 RGB 顏色

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

**說明：**
- 灰色文字，50% 透明度
- 可自定義任何顏色

---

#### 示例 4：完全透明

```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0
  }
}
```

**說明：**
- 完全透明
- 某些閱讀器可能無法搜索

---

## font 配置（字體設置）

### 參數說明

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `path` | String | ⚪ | 系統默認 | TrueType 字體文件路徑（**只支援 .ttf**，不支援 .ttc） |

### ⚠️ 重要：字體格式限制

**只支援 `.ttf` 格式，不支援 `.ttc` 格式！**

PDFBox 無法正確處理 TTC（TrueType Collection）字體。請使用 TTF 格式：

| 字體 | 格式 | 支援 | 說明 |
|------|------|------|------|
| `kaiu.ttf`（標楷體） | TTF | ✅ | **推薦**，支援繁體中文 |
| `simhei.ttf`（黑體） | TTF | ✅ | 支援簡體中文 |
| `arial.ttf` | TTF | ✅ | 支援英文 |
| `msyh.ttc`（微軟雅黑） | **TTC** | ❌ | **不支援！** |
| `msjh.ttc`（正黑體） | **TTC** | ❌ | **不支援！** |
| `simsun.ttc`（宋體） | **TTC** | ❌ | **不支援！** |

### 默認字體（按優先級自動選擇）

當未指定 `font.path` 時，程序會按以下順序尋找可用字體：

| 優先級 | 系統 | 字體路徑 | 字體名稱 |
|-------|------|---------|---------|
| 1 | Windows | `C:/Windows/Fonts/simhei.ttf` | 黑體（TTF ✅） |
| 2 | Windows | `C:/Windows/Fonts/arial.ttf` | Arial（TTF ✅） |
| 3 | Windows | `C:/Windows/Fonts/meiryo.ttc` | Meiryo（TTC ❌） |
| 4 | Windows | `C:/Windows/Fonts/msyh.ttc` | 微軟雅黑（TTC ❌） |

> **建議**：CJK 文件請明確指定 `font.path`，避免使用默認字體。

### 配置示例

#### 示例 1：使用標楷體（繁體中文，推薦）

```json
{
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

**說明：**
- 標楷體為 TTF 格式，支援繁體中文 + 英文
- **推薦用於繁體中文文件**

---

#### 示例 2：使用黑體（簡體中文）

```json
{
  "font": {
    "path": "C:/Windows/Fonts/simhei.ttf"
  }
}
```

**說明：**
- 黑體為 TTF 格式，支援簡體中文 + 英文

---

#### 示例 3：使用系統默認（不指定）

```json
{
  // 不需要 font 配置，會自動選擇系統字體
}
```

**說明：**
- 程序會自動選擇合適的字體
- 推薦使用此方式

---

## 完整示例

### 示例 1：處理繁體中文文檔（多頁，含簡繁轉換）

```json
{
  "input": {
    "folder": "C:/Documents/Chinese",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/Output/Chinese",
    "format": "all",
    "multiPage": true
  },
  "ocr": {
    "language": "chinese_cht",
    "cpuThreads": 4
  },
  "textConvert": "s2t",
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

**適用場景：**
- ✅ 處理繁體中文文檔
- ✅ 合併多頁為一個文件
- ✅ 生成 PDF + OFD + TXT 三種格式

**預期輸出：**
```
C:/Output/Chinese/
  multipage_20260323_130000.pdf
  multipage_20260323_130000.ofd
  multipage_20260323_130000.txt
```

---

### 示例 2：處理英文文檔（單頁）

```json
{
  "input": {
    "folder": "C:/Documents/English",
    "extensions": ["jpg", "jpeg", "png"]
  },
  "output": {
    "folder": "C:/Output/English",
    "format": "pdf,txt",
    "multiPage": false
  },
  "ocr": {
    "language": "en"
  }
}
```

**適用場景：**
- ✅ 處理英文文檔
- ✅ 每個圖片生成獨立的 PDF + TXT
- ✅ 支持多種圖片格式

**預期輸出：**
```
C:/Output/English/
  doc1_20260323_130000.pdf
  doc1_20260323_130000.txt
  doc2_20260323_130001.pdf
  doc2_20260323_130001.txt
```

---

### 示例 3：處理混合語言文檔

```json
{
  "input": {
    "folder": "C:/Documents/Mixed",
    "pattern": "*.*"
  },
  "output": {
    "folder": "C:/Output/Mixed",
    "format": "pdf,ofd",
    "multiPage": true
  },
  "ocr": {
    "language": "chinese_cht",
    "cpuThreads": 8
  },
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

**適用場景：**
- ✅ 繁體中文 + 英文混合文檔
- ✅ 只生成 PDF + OFD（無 TXT）
- ✅ 使用標楷體

**預期輸出：**
```
C:/Output/Mixed/
  multipage_20260323_130000.pdf
  multipage_20260323_130000.ofd
```

---

### 示例 4：高性能配置

```json
{
  "input": {
    "folder": "C:/OCR/Watch",
    "pattern": "*.*"
  },
  "output": {
    "folder": "C:/OCR/Output",
    "format": "pdf,ofd",
    "multiPage": true
  },
  "ocr": {
    "language": "en",
    "useGpu": true,
    "cpuThreads": 16
  },
  "font": {
    "path": "C:/Windows/Fonts/arial.ttf"
  }
}
```

**適用場景：**
- ✅ 高性能機器（有 GPU）
- ✅ 大批量處理
- ✅ 使用 GPU 加速

---

### 示例 5：處理單個文件

```json
{
  "input": {
    "file": "C:/Documents/contract.jpg"
  },
  "output": {
    "folder": "C:/Output",
    "format": "all",
    "multiPage": false
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

**適用場景：**
- ✅ 只處理單個文件
- ✅ 生成所有格式

**預期輸出：**
```
C:/Output/
  contract_20260323_130000.pdf
  contract_20260323_130000.ofd
  contract_20260323_130000.txt
```

---

## 注意事項

### 1. 路徑格式

```json
// ✅ 推薦：正斜線（跨平台兼容）
{
  "input": {
    "folder": "C:/OCR/Watch"
  }
}

// ⚠️ 可用：雙反斜線（僅 Windows）
{
  "input": {
    "folder": "C:\\OCR\\Watch"
  }
}

// ❌ 錯誤：單反斜線（會轉義錯誤）
{
  "input": {
    "folder": "C:\OCR\Watch"  // ❌ 錯誤！
  }
}
```

### 2. JSON 語法

```json
// ✅ 正確示例
{
  "output": {
    "format": "pdf,ofd",  // 最後一項無逗號
    "multiPage": true            // 最後一個屬性無逗號
  }
}

// ❌ 錯誤示例
{
  "output": {
    "formats": ["pdf", "ofd",],  // ❌ 數組最後多逗號
    "multiPage": true,           // ❌ 對象最後多逗號
  }
}
```

### 3. 編碼問題

```
✅ 推薦：UTF-8 編碼保存配置文件
⚠️ 避免：路徑中包含中文字符（部分系統可能不支持）
⚠️ 避免：路徑中包含特殊字符（空格、符號等）
```

### 4. 性能優化建議

```
圖片大小建議配置：
- 小圖片（< 2MP）:  -Xmx1G, cpuThreads: 2
- 中等圖片（2-5MP）: -Xmx2G, cpuThreads: 4
- 大圖片（> 5MP）:   -Xmx4G, cpuThreads: 8
- 批量處理:         -Xmx4G, multiPage: true
```

### 5. 常見問題

#### Q1: 沒有生成 OFD 或 TXT 文件？

**可能原因：**
- 字體加載失敗
- 沒有包含在 `formats` 中

**解決方法：**
```json
{
  "output": {
    "format": "all"  // 確保包含所需格式
  },
  "font": {
    "path": "C:/Windows/Fonts/arial.ttf"  // 嘗試使用其他字體
  }
}
```

#### Q2: OCR 識別出簡繁混合？

**可能原因：**
- RapidOCR 的 `chinese_cht` 模型仍可能輸出簡體字

**解決方法：**
```json
{
  "textConvert": "s2t"  // 自動將簡體轉為繁體
}
```

#### Q3: OCR 識別準確度低？

**解決方法：**
```bash
# 增加 JVM 內存
java -Xmx4G -jar jpeg2pdf-ofd.jar config.json

# 增加 CPU 線程
{
  "ocr": {
    "cpuThreads": 8
  }
}

# 啟用 GPU 加速（如果有 GPU）
{
  "ocr": {
    "useGpu": true
  }
}
```

---

**GitHub:** https://github.com/brianshih04/jpeg2pdf-ofd-conveyor

**更新時間：** 2026-03-25
