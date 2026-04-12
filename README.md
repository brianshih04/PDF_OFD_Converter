# JPEG2PDF-OFD-OCR

**跨平台 OCR 工具：將 JPEG 圖片轉換為可搜索的 PDF/OFD 文件**

架構由兩個組件組成：**Java CLI 引擎**（OCR 核心處理）+ **Python pywebview UI**（圖形化操作介面）。

[![GitHub](https://img.shields.io/badge/GitHub-brianshih04%2FPDF_OFD_Converter-blue)](https://github.com/brianshih04/PDF_OFD_Converter)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)
[![Python](https://img.shields.io/badge/Python-3.12+-blue)](https://www.python.org/)
[![Version](https://img.shields.io/badge/Version-v0.21-blue)]()

---

## 架構概覽

```
┌─────────────────────────────────────┐
│     Python UI Shell (pywebview)      │
│  pdf-converter-ui/                   │
│  ├── app.py          # 進入點        │
│  ├── core/bridge.py  # Java CLI 橋接 │
│  ├── core/config.py  # 設定管理       │
│  └── api/js_api.py   # JS↔Python 橋接 │
└──────────────┬──────────────────────┘
               │ subprocess (JSON stdout)
┌──────────────▼──────────────────────┐
│     Java CLI Engine                  │
│  Java 21 + Maven                     │
│  ├── Main.java       # CLI 進入點    │
│  ├── OcrService.java  # OCR 核心    │
│  └── PdfService.java  # PDF 產生    │
└─────────────────────────────────────┘
```

- **Dev 模式**：`python app.py` 啟動 Python UI，自動呼叫 Java JAR
- **Production 模式**：PyInstaller EXE + 內嵌 JAR，單一可執行檔

## 功能特色

- **Windows 便攜版**：ZIP 解壓即用，無需安裝
- **Python pywebview UI**：輕量桌面 GUI，無需 JavaFX
- **80+ 種 OCR 語言**：支援繁中、簡中、英文、日文、韓文等
- **多種輸出格式**：PDF、OFD（中國國家標準）、TXT
- **單頁/多頁模式**：彈性的輸出選項
- **純 Java SE**：無 Spring Boot 依賴，輕量快速
- **可搜索 PDF/OFD**：使用逐字符定位算法，精確對齊文字層
- **直列文字支援**：自動偵測並正確繪製直排文字
- **智慧字型選擇**：根據 OCR 語言自動選擇對應 CJK 字型（NotoSans TC/SC）
- **Tesseract OCR 支援**：對 RapidOCR 識別不佳的語言（泰文、俄文、西里爾語系、阿拉伯語系、希臘文、印地文等）自動切換到 Tesseract 引擎
- **簡繁轉換**：使用 OpenCC 在生成前自動轉換簡體/繁體中文

---

## 版本比較

| 版本 | 大小 | 需求 | 平台 | 說明 |
|------|------|------|------|------|
| **PyInstaller EXE (GUI)** | **~350 MB** | **無需 Java/Python** | **Windows x64** | **便攜版，解壓即用** |
| **JAR (CLI only)** | **82 MB** | **Java 17+** | 所有平台 | 純命令行模式 |
| **Dev 模式 (GUI)** | - | **Python 3.12+ + Java 21** | 所有平台 | `python app.py` |

---

## 快速開始

### Windows（推薦）

1. 下載 `JPEG2PDF-OFD-OCR-v0.21-windows-x64.zip`
2. 解壓縮到任意資料夾
3. **GUI 模式**：雙擊 `start.bat` 或 `JPEG2PDF-OFD-OCR.exe`
4. **CLI 模式**：開啟命令提示字元，執行：
   ```powershell
   java -jar jpeg2pdf-ofd-nospring-0.21.jar config.json
   ```

### CLI (JAR) — 跨平台

```bash
# 建置
mvn clean package

# 執行
java -jar target/jpeg2pdf-ofd-nospring-0.21.jar config.json
```

---

## 字體下載指南

### 推薦字體：Noto Sans CJK（免費開源）

**Noto Sans CJK** 是 Google 和 Adobe 合作開發的免費開源字體，支持**繁中、簡中、日文、韓文**四種語言。

#### 下載連結

| 語言 | GitHub Release | Google Fonts |
|------|----------------|--------------|
| **繁體中文 (TC)** | [NotoSansTC-hinted.zip](https://github.com/googlefonts/noto-cjk/releases) | [Google Fonts TC](https://fonts.google.com/noto/specimen/Noto+Sans+TC) |
| **簡體中文 (SC)** | [NotoSansSC-hinted.zip](https://github.com/googlefonts/noto-cjk/releases) | [Google Fonts SC](https://fonts.google.com/noto/specimen/Noto+Sans+SC) |
| **日文 (JP)** | [NotoSansJP-hinted.zip](https://github.com/googlefonts/noto-cjk/releases) | [Google Fonts JP](https://fonts.google.com/noto/specimen/Noto+Sans+JP) |
| **韓文 (KR)** | [NotoSansKR-hinted.zip](https://github.com/googlefonts/noto-cjk/releases) | [Google Fonts KR](https://fonts.google.com/noto/specimen/Noto+Sans+KR) |
| **所有語言 (OTF)** | [NotoSansCJK-Regular.ttc](https://github.com/googlefonts/noto-cjk/releases) | - |

#### 下載方式

##### 方式 1：從 GitHub Release 下載（推薦）

```bash
# 下載最新版本（所有語言）
https://github.com/googlefonts/noto-cjk/releases

# 或使用命令行下載
wget https://github.com/googlefonts/noto-cjk/releases/download/Sans2.004/NotoSansCJK-Regular.ttc
```

##### 方式 2：從 Google Fonts 下載

```
https://fonts.google.com/noto/specimen/Noto+Sans+TC
https://fonts.google.com/noto/specimen/Noto+Sans+SC
https://fonts.google.com/noto/specimen/Noto+Sans+JP
https://fonts.google.com/noto/specimen/Noto+Sans+KR
```

##### 方式 3：作業系統自帶字體

| 作業系統 | 繁體中文 | 簡體中文 | 日文 | 韓文 |
|---------|---------|---------|------|------|
| **Windows** | `C:/Windows/Fonts/kaiu.ttf` (標楷體) | `C:/Windows/Fonts/simsun.ttc` (宋體) | - | - |
| **Linux** | `/usr/share/fonts/noto/NotoSansCJK-Regular.ttc` | 同左 | 同左 | 同左 |
| **macOS** | `/System/Library/Fonts/PingFang.ttc` (苹方) | 同左 | `/System/Library/Fonts/Hiragino.ttc` | - |

#### 安裝字體

##### Windows

```powershell
# 方法 1：雙擊 .ttf 或 .otf 文件，點擊「安裝」

# 方法 2：複製到字體資料夾
Copy-Item NotoSansTC-Regular.otf C:\Windows\Fonts\
```

##### Linux (Ubuntu/Debian)

```bash
# 安裝 Noto CJK 字體
sudo apt-get install fonts-noto-cjk

# 或手動安裝
mkdir -p ~/.local/share/fonts
cp NotoSansCJK-Regular.ttc ~/.local/share/fonts/
fc-cache -fv
```

##### macOS

```bash
# 雙擊 .ttf 或 .otf 文件，點擊「安裝字體」

# 或複製到字體資料夾
cp NotoSansTC-Regular.otf ~/Library/Fonts/
```

#### 配置示例

##### 繁體中文配置

```json
{
  "ocr": {
    "language": "chinese_cht"
  },
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

##### 簡體中文配置

```json
{
  "ocr": {
    "language": "ch"
  },
  "font": {
    "path": "C:/Windows/Fonts/simsun.ttc"
  }
}
```

##### 日文配置

```json
{
  "ocr": {
    "language": "japan"
  },
  "font": {
    "path": "/usr/share/fonts/noto/NotoSansCJK-Regular.ttc"
  }
}
```

##### 韓文配置

```json
{
  "ocr": {
    "language": "korean"
  },
  "font": {
    "path": "/usr/share/fonts/noto/NotoSansCJK-Regular.ttc"
  }
}
```

#### 萬用字體推薦：GoNotoKurrent（支援所有語言）

**[GoNotoKurrent](https://github.com/satbyy/go-noto-universal/releases)** 是一個整合型字體，單一 TTF 檔案即可支援 **80+ 種現代文字系統**，包括 CJK、泰文、阿拉伯文、希臘文、印地文、西里爾語系等。

- 檔案：`GoNotoKurrent-Regular.ttf`（約 15.5 MB）
- 授權：SIL OFL 1.1（免費商用）
- 下載：[GitHub Releases](https://github.com/satbyy/go-noto-universal/releases)

```json
{
  "font": {
    "path": "C:/Fonts/GoNotoKurrent-Regular.ttf"
  }
}
```

> **提示**：如果需要處理多種非 CJK 語言（泰文、阿拉伯文、希臘文等），強烈推薦使用 GoNotoKurrent，免去逐一下載各語言字體的麻煩。

#### 自動字體選擇（無需配置）

本工具支持**自動字體選擇**，根據 OCR 語言自動選擇對應的 Noto Sans CJK 字體：

| OCR 語言 | 自動選擇字體 |
|---------|-------------|
| `chinese_cht` | NotoSansTC |
| `ch` / `cn` | NotoSansSC |
| `japan` | NotoSansJP |
| `korean` | NotoSansKR |

**注意**：自動選擇需要系統已安裝 Noto Sans CJK 字體。

#### 授權

- **Noto Sans CJK**: SIL Open Font License 1.1（免費商用）
- 官方網站：https://fonts.google.com/noto
- GitHub：https://github.com/googlefonts/noto-cjk

---

## 相關文檔

- **[JSON 配置完整指南](JSON-CONFIG-GUIDE.md)** - 所有 JSON 配置選項的詳細說明（含 textLayer 配置）
- **[Searchable OFD 技術筆記](SEARCHABLE_OFD_NOTES.md)** - Searchable PDF/OFD 產生方法與技術實現細節
- **[開發者指南](DEVELOPER-GUIDE.md)** - 建置、開發、打包與字體設定

---

## 配置說明

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
  },
  "textConvert": "s2t",
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
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
- `chinese_cht` - 繁體中文（預設，使用 NotoSansTC）
- `ch` / `cn` - 簡體中文（使用 NotoSansSC）
- `en` - 英文
- `japan` - 日文
- `korean` - 韓文
- 以及其他 75+ 種語言（RapidOCR 引擎）

**以下語言自動使用 Tesseract OCR 引擎（無需手動切換）：**

| 語言 | 語言代碼 | Tesseract 模型 |
|------|---------|---------------|
| Hebrew 希伯來文 | `he`, `hebrew` | heb+eng |
| Thai 泰文 | `th`, `tha`, `thai` | tha+eng |
| Russian 俄文 | `ru`, `rus`, `russian` | rus+eng |
| Ukrainian 烏克蘭文 | `uk`, `ukr`, `ukrainian` | ukr+eng |
| Bulgarian 保加利亞文 | `bg`, `bul`, `bulgarian` | bul+eng |
| Serbian 塞爾維亞文 | `sr`, `srp`, `serbian` | srp+eng |
| Macedonian 馬其頓文 | `mk`, `mkd`, `macedonian` | mkd+eng |
| Belarusian 白俄羅斯文 | `be`, `bel`, `belarusian` | bel+eng |
| Greek 希臘文 | `el`, `ell`, `gre`, `greek`, `grc` | ell+eng |
| Hindi 印地文 | `hi`, `hin`, `hindi` | hin+eng |
| Gujarati 古吉拉特文 | `gu`, `guj`, `gujarati` | guj+eng |
| Persian 波斯文 | `fa`, `fas`, `persian`, `farsi` | ara+eng |
| Arabic 阿拉伯文 | `ar`, `ara`, `arabic` | ara+eng |

#### textConvert 配置（簡繁轉換）

OCR 識別結果可能混合簡繁體，可使用 OpenCC 自動轉換：

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `textConvert` | String | ❌ | `null`（不轉換） | `"s2t"` 簡→繁，`"t2s"` 繁→簡 |

**範例：簡體轉繁體**
```json
{
  "ocr": { "language": "chinese_cht" },
  "textConvert": "s2t"
}
```

**範例：繁體轉簡體**
```json
{
  "ocr": { "language": "ch" },
  "textConvert": "t2s"
}
```

> **提示**：即使 OCR 語言設為 `chinese_cht`，RapidOCR 的輸出仍可能混合簡體字（如「价」→「價」、「帐」→「帳」）。加上 `"textConvert": "s2t"` 可確保所有文字都是繁體。

#### textLayer 配置

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `color` | String | ❌ | `"white"` | 文字層顏色名稱 |
| `red` | Integer | ❌ | `255` | RGB 紅色值 (0-255) |
| `green` | Integer | ❌ | `255` | RGB 綠色值 (0-255) |
| `blue` | Integer | ❌ | `255` | RGB 藍色值 (0-255) |
| `opacity` | Double | ❌ | `0.0001` | 透明度 (0.0 - 1.0) |

**支持的顏色名稱：**
- `"white"` - 白色（預設，生產環境）
- `"debug"` - 調試模式（紅色不透明）
- `"red"` - 紅色
- `"black"` - 黑色
- `"blue"` - 藍色
- `"green"` - 綠色

**調試模式：**
```json
{
  "textLayer": {
    "color": "debug"
  }
}
```

**生產模式：**
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

---

## 使用範例

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

### 範例 3：調試模式

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
  },
  "textLayer": {
    "color": "debug"
  }
}
```

**效果：**
- 紅色不透明文字層
- 方便觀察文字定位是否準確
- 適合開發和測試

---

## 測試結果

**測試輸入：**
- 1 張 JPEG 圖片
- OCR 語言：繁體中文
- 配置：調試模式（紅色文字層）

**測試輸出：**
```
Sample_20260324_093624.pdf (2.69 MB)
Sample_20260324_090839.ofd (2.64 MB)

處理時間：約 30 秒
OCR 偵測：52 個文字區塊
文字層定位：精確對齊
WPS 搜索：可搜索
```

---

## 從原始碼建置

### 先決條件

- JDK 21+（編譯 Java CLI 引擎）
- Maven 3.6+
- Python 3.12+（UI 開發，pythonnet 不支援 3.14，建議用 3.12）
- pywebview（`pip install pywebview`）

### Dev 模式啟動

```bash
# 1. 建置 Java JAR
mvn clean package -DskipTests

# 2. 建立 Python 虛擬環境（推薦 3.12，pythonnet 不支援 3.14）
cd pdf-converter-ui
uv venv -p 3.12 .venv
uv pip install pywebview

# 3. 啟動 Python UI（自動橋接 Java CLI）
.venv\Scripts\python app.py   # Windows
# source .venv/bin/activate && python app.py  # Linux/macOS
```

### 建置 JAR

```bash
mvn clean package
```

輸出：`target/jpeg2pdf-ofd-nospring-0.21.jar`

### 建置 Windows 便攜版 (ZIP)

```bash
# 1. 建置 Java JAR
mvn clean package -DskipTests

# 2. PyInstaller 打包 Python UI + JAR
cd pdf-converter-ui/build
pyinstaller --onefile --add-data "../ui;ui" ../app.py
```

輸出：`dist/app.exe`（含內嵌 JAR）

> **注意：** 若僅修改 Java 程式碼而未改動依賴，可直接複製 JAR 到部署目錄的 `app/` 資料夾覆蓋，無需每次重新打包。

---

## 已知限制

- **特殊符號缺字**：GoNotoKurrent 字體不包含 ≤ ≥ △ ℃ μ 等數學/科學符號，OCR 識別正常但 PDF 文字層會跳過這些字元。若需完整符號支援，可在 config.json 的 `fontPath` 指定包含符號的字體（如 Noto Sans CJK）。
- **Python 版本**：GUI 需要 Python 3.12+（pythonnet 不支援 3.14，建議使用 `uv venv -p 3.12`）。
- **字體路徑**：Bridge 自動設定 cwd 為專案根目錄，Java 可從相對路徑 `fonts/GoNotoKurrent-Regular.ttf` 載入字體。若需自訂字體，在 UI 設定頁或 config.json 設定 `fontPath` 為絕對路徑。

---

## 文件列表

- **README.md** - 本文件
- **JSON-CONFIG-GUIDE.md** - JSON 配置指南
- **SEARCHABLE_OFD_NOTES.md** - Searchable PDF/OFD 技術筆記
- **DEVELOPER-GUIDE.md** - 開發者指南（建置、打包、字體）
- **PLAN_NEW_UI.md** - 新 UI 架構遷移計畫
- **pdf-converter-ui/** - Python UI 專案
- **CHINA-LINUX-GUIDE.md** - 中國國產 Linux 構建指南
- **build-china-linux.sh** - Linux/macOS 構建腳本
- **build-china-linux.ps1** - Windows PowerShell 構建腳本

---

## 總結

**完整功能：**
- OCR 識別（80+ 種語言）
- 可搜索 PDF 產生（PDFBox 2.0.29）
- 可搜索 OFD 產生（ofdrw 2.3.8）
- TXT 匯出
- 單頁模式
- 多頁模式
- 逐字符定位算法（精確對齊文字層）
- 直列文字偵測與繪製（自動判斷 height > width * 1.5）
- 智慧字型 fallback（config → NotoSans CJK → 系統字型）
- 自定義文字層顏色和透明度
- OpenCC 簡繁轉換（s2t/t2s）

**打包選項：**
- PyInstaller EXE（推薦，含 GUI+CLI，Windows 便攜版）
- JAR（CLI only，需要 Java）

---

**GitHub：** https://github.com/brianshih04/PDF_OFD_Converter
