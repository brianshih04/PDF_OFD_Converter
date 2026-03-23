# JPEG2PDF-OFD OCR CLI

**跨平台 OCR 工具：將 JPEG 圖片轉換為可搜尋的 PDF/OFD 文件**

[![GitHub](https://img.shields.io/badge/GitHub-brianshih04%2Fjpeg2pdf--ofd--conveyor-blue)](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange)](https://adoptium.net/)

---

## ✨ 功能特色

- ✅ **跨平台支援**：Windows、macOS (Intel/ARM)、Linux
- ✅ **無需安裝 Java**：使用 Conveyor 打包，自包含執行環境
- ✅ **80+ 種 OCR 語言**：支援繁中、簡中、英文、日文、韓文等
- ✅ **多種輸出格式**：PDF、OFD（中國國家標準）、TXT
- ✅ **單頁/多頁模式**：彈性的輸出選項
- ✅ **自動更新**：內建更新機制（Conveyor）
- ✅ **純 Java SE**：無 Spring Boot 依賴，輕量快速

---

## 📊 版本比較

| 版本 | 大小 | 需求 | 平台 | 自動更新 | 推薦度 |
|------|------|------|------|----------|--------|
| **Conveyor** | **~78 MB** | **無需 Java** | **4 個平台** | ✅ | **⭐⭐⭐⭐⭐** |
| **JAR** | **52 MB** | **Java 17+** | 所有平台 | ❌ | ⭐⭐⭐ |
| **jpackage** | **181 MB** | **無需 Java** | Windows only | ❌ | ⭐⭐ |

---

## 🚀 快速開始

### Windows

#### 方法 1：下載 MSIX（推薦）

1. 下載：[jpeg2pdf-ofd-cli-3.0.0.x64.msix](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor/releases)
2. 雙擊安裝
3. 在 PowerShell 中執行：
   ```powershell
   jpeg2pdf-ofd config.json
   ```

#### 方法 2：PowerShell 一鍵安裝（自動更新）

```powershell
iex (irm https://brianshih04.github.io/jpeg2pdf-ofd-conveyor/install.ps1)
```

### macOS

```bash
# 下載適合的版本
# Intel Mac: jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip
# Apple Silicon: jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip

unzip jpeg2pdf-ofd-cli-3.0.0-mac-*.zip
./jpeg2pdf-ofd-cli config.json
```

### Linux

```bash
# DEB (Ubuntu/Debian)
sudo dpkg -i brian-shih-jpeg2pdf-ofd-cli_3.0.0_amd64.deb

# TAR.GZ (通用)
tar xzf jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz
./jpeg2pdf-ofd-cli config.json
```

---

## ⚙️ 配置說明

### 完整配置範例

```json
{
  "input": {
    "folder": "C:/OCR/Input",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/OCR/Output",
    "formats": ["pdf", "ofd", "txt"],
    "multiPage": true
  },
  "ocr": {
    "language": "chinese_cht",
    "cpuThreads": 4
  }
}
```

### 配置參數說明

#### input 配置

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `folder` | String | ✅ | - | 輸入圖片資料夾路徑 |
| `file` | String | ❌ | - | 單一檔案路徑 |
| `pattern` | String | ❌ | `*.jpg` | 檔案過濾模式 |
| `extensions` | Array | ❌ | `["jpg", "jpeg", "png"]` | 支援的副檔名 |

#### output 配置

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `folder` | String | ✅ | - | 輸出資料夾路徑 |
| `formats` | Array | ❌ | `["pdf"]` | 輸出格式：`"pdf"`, `"ofd"`, `"txt"` |
| `multiPage` | Boolean | ❌ | `false` | 合併為多頁文件 |

**formats 選項：**
- `["pdf"]` - 僅 PDF
- `["ofd"]` - 僅 OFD（中國國家標準）
- `["txt"]` - 僅純文字
- `["pdf", "ofd"]` - PDF + OFD
- `["pdf", "ofd", "txt"]` - 所有格式

#### ocr 配置

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `language` | String | ❌ | `chinese_cht` | OCR 語言 |
| `useGpu` | Boolean | ❌ | `false` | 使用 GPU 加速 |
| `cpuThreads` | Integer | ❌ | `4` | CPU 執行緒數 |

**支援的語言（80+ 種）：**
- `chinese_cht` - 繁體中文（預設）
- `ch` - 簡體中文
- `en` - 英文
- `japan` - 日文
- `korean` - 韓文
- 以及其他 75+ 種語言...

---

## 📖 使用範例

### 範例 1：繁體中文多頁文件

```json
{
  "input": {
    "folder": "C:/Documents/Chinese",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/Output",
    "formats": ["pdf", "ofd", "txt"],
    "multiPage": true
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

**輸出：**
- 1 個多頁 PDF（所有頁面合併）
- 1 個多頁 OFD（所有頁面合併）
- 1 個 TXT 檔案（所有文字提取）

### 範例 2：英文文件（單頁模式）

```json
{
  "input": {
    "folder": "C:/Documents/English"
  },
  "output": {
    "folder": "C:/Output",
    "formats": ["pdf"],
    "multiPage": false
  },
  "ocr": {
    "language": "en"
  }
}
```

**輸出：**
- 每張圖片一個 PDF 檔案

### 範例 3：單一檔案處理

```json
{
  "input": {
    "file": "C:/Documents/scan.jpg"
  },
  "output": {
    "folder": "C:/Output",
    "formats": ["pdf", "ofd"]
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

---

## ✅ 測試結果

**測試輸入：**
- 8 張 JPEG 圖片（Computershare 匯款表單）
- 位置：`P:\OCR\Sample\`
- 配置：`config-multipage-example.json`

**測試輸出：**
```
✅ multipage_20260323_203921.pdf (14.96 MB, 8 頁)
✅ multipage_20260323_203921.ofd (14.60 MB, 8 頁)

處理時間：約 60 秒
OCR 偵測：543 個文字區塊
```

---

## 🔧 已修復的問題

### 問題 1：配置解析錯誤 ✅ 已修復

**問題：**
- 只產生 PDF
- OFD 和 TXT 未產生

**原因：**
```java
// Main.java 只檢查 "format" 鍵
if (outputConfig.containsKey("format"))

// 但配置使用 "formats"（複數）
"formats": ["pdf", "ofd"]
```

**修復：**
```java
// 同時支援 "formats"（複數）和 "format"（單數）
Object formats = outputConfig.get("formats");
if (formats == null) {
    formats = outputConfig.get("format"); // 向後相容
}
```

---

### 問題 2：PDF 字體載入失敗 ✅ 已修復

**問題：**
```
Warning: Cannot load font from C:/Windows/Fonts/msyh.ttc
```

**修復：**
```java
// 嘗試多種字體
String[] fonts = {
    "C:/Windows/Fonts/arial.ttf",
    "C:/Windows/Fonts/simhei.ttf",
    "C:/Windows/Fonts/simsun.ttc",
    "C:/Windows/Fonts/msyh.ttc"
};
for (String path : fonts) {
    try {
        return PDType0Font.load(document, new File(path));
    } catch (Exception e) {
        continue; // 嘗試下一個
    }
}
return PDType1Font.HELVETICA; // 最終備援
```

---

### 問題 3：PDF 文字渲染錯誤 ✅ 已修復

**問題：**
```
Error: Nested beginText() calls are not allowed
```

**修復：**
```java
try {
    contentStream.beginText();
    contentStream.setFont(font, fontSize);
    contentStream.showText(text);
} finally {
    contentStream.endText(); // 永遠會被呼叫
}
```

---

### 問題 4：OFD 多頁產生失敗 ✅ 已修復

**問題：**
- OFD 檔案產生但大小為 0

**修復：**
- 配置解析修正
- OFD 現在可以正確產生

---

## 🔨 從原始碼建置

### 先決條件

- JDK 17+
- Maven 3.6+
- Conveyor（用於跨平台打包）

### 建置 JAR

```bash
mvn clean package
```

輸出：`target/jpeg2pdf-ofd-nospring-3.0.0-jar-with-dependencies.jar`

### 建置 Conveyor 跨平台套件

```bash
# 安裝 Conveyor
# Windows: choco install conveyor
# macOS: brew install --cask conveyor
# Linux: https://www.hydraulic.software/download

# 建置所有平台
conveyor make site
```

輸出：
- `output/jpeg2pdf-ofd-cli-3.0.0.x64.msix` (Windows)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip` (macOS Intel)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip` (macOS ARM)
- `output/jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz` (Linux)

---

## 📚 文件

- **README.md** - 本文件
- **conveyor.conf** - Conveyor 配置
- **CONVEYOR-GUIDE.md** - 完整 Conveyor 指南
- **JSON-CONFIG-GUIDE.md** - JSON 配置指南

---

## 📋 總結

**完整功能：**
- OCR 識別（80+ 種語言）
- PDF 產生（PDFBox 2.0.29）
- OFD 產生（ofdrw 2.3.8）
- TXT 匯出
- 單頁模式
- 多頁模式

**跨平台支援：**
- Windows
- macOS (Intel + ARM)
- Linux (Debian + RPM)

**打包選項：**
- Conveyor（推薦） ⭐⭐⭐⭐⭐
- jpackage（僅 Windows）
- JAR（需要 Java）

**所有問題已修復：**
- 配置解析
- PDF 字體
- PDF 文字渲染
- OFD 多頁產生

**測試結果：**
- 8 張圖片 → 1 個多頁 PDF + 1 個多頁 OFD

---

**GitHub：** https://github.com/brianshih04/jpeg2pdf-ofd-conveyor
