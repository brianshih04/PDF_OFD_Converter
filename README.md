# JPEG2PDF-OFD OCR

**跨平台 OCR 工具：將 JPEG 圖片轉換為可搜索的 PDF/OFD 文件**

支援 **GUI（雙擊啟動）** 與 **CLI（命令行）** 兩種使用方式。

[![GitHub](https://img.shields.io/badge/GitHub-brianshih04%2FPDF_OFD_Converter-blue)](https://github.com/brianshih04/PDF_OFD_Converter)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange)](https://adoptium.net/)

---

## 功能特色

- ✅ **跨平台支援**：Windows、macOS (Intel/ARM)、Linux
- ✅ **無需安裝 Java**：使用 Conveyor 打包，自包含執行環境
- ✅ **80+ 種 OCR 語言**：支援繁中、簡中、英文、日文、韓文等
- ✅ **多種輸出格式**：PDF、OFD（中國國家標準）、TXT
- ✅ **單頁/多頁模式**：彈性的輸出選項
- ✅ **自動更新**：內建更新機制（Conveyor）
- ✅ **純 Java SE**：無 Spring Boot 依賴，輕量快速
- ✅ **可搜索 PDF/OFD**：使用逐字符定位算法，精確對齊文字層
- ✅ **直列文字支援**：自動偵測並正確繪製直排文字
- ✅ **智慧字型選擇**：根據 OCR 語言自動選擇對應 CJK 字型（NotoSans TC/SC）
- ✅ **Tesseract OCR 支援**：對 RapidOCR 識別不佳的語言（泰文、俄文、西里爾語系、阿拉伯語系、希臘文、印地文等）自動切換到 Tesseract 引擎
- ✅ **簡繁轉換**：使用 OpenCC 在生成前自動轉換簡體/繁體中文

---

## 版本比較

| 版本 | 大小 | 需求 | 平台 | 自動更新 | 推薦度 |
|------|------|------|------|----------|--------|
| **Conveyor** | **~78 MB** | **無需 Java** | **4 個平台** | ✅ | **⭐⭐⭐⭐⭐** |
| **JAR** | **52 MB** | **Java 17+** | 所有平台 | ❌ | ⭐⯠⭐ |
| **jpackage** | **181 MB** | **無需 Java** | Windows only | ❌ | ⭐⭐ |

---

## 快速開始

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
iex (irm https://brianshih04.jpeg2pdf-ofd-conveyor/install.ps1)
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

> 💡 **提示**：如果需要處理多種非 CJK 語言（泰文、阿拉伯文、希臘文等），強烈推薦使用 GoNotoKurrent，免去逐一下載各語言字體的麻煩。

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

- **[JSON 配置完整指南](JSON-CONFIG-GUIDE.md)** - 所有 JSON 配置選項的詳細說明
- **[Searchable OFD 完整產生方法](searchable_method.md)** - 如何生成可搜索的 OFD 文件
- **[技術筆記](SEARCHABLE_OFD_NOTES.md)** - Searchable OFD 技術實現細節
- **[textLayer 配置指南](TEXTLAYER-CONFIG-GUIDE.md)** - 如何配置文字層顏色和透明度

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

> 💡 **提示**：即使 OCR 語言設為 `chinese_cht`，RapidOCR 的輸出仍可能混合簡體字（如「价」→「價」、「帐」→「帳」）。加上 `"textConvert": "s2t"` 可確保所有文字都是繁體。

#### textLayer 配置（新功能）

| 參數 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `color` | String | ❌ | `"white"` | 文字層顏色名稱 |
| `red` | Integer | ❌ | `255` | RGB 紅色值 (0-255) |
| `green` | Integer | ❌ | `255` | RGB 綠色值 (0-255) |
| `blue` | Integer | ❌ | `255` | RGB 藍色值 (0-255) |
| `opacity` | Double | ❌ | `0.0001` | 透明度 (0.0 - 1.0) |

**支持的顏色名稱：**
- `"white"` - 白色（默認，生產環境）
- `"debug"` - 調試模式（紅色不透明）
- `"red"` - 紅色
- `"black"` - 黑色
- `"blue"` - 藍色
- `"green"` - 綠色

**調試模式：**
```json
{
  "textLayer": {
    "color": "debug"  // 紅色 + 不透明，方便觀察文字定位
  }
}
```

**生產模式：**
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001  // 極低透明度，可搜索但幾乎看不見
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
✅ Sample_20260324_093624.pdf (2.69 MB)
✅ Sample_20260324_090839.ofd (2.64 MB)

處理時間：約 30 秒
OCR 偵測：52 個文字區塊
文字層定位：精確對齊
WPS 搜索：✅ 可搜索
```

---

## 從原始碼建置

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
# 安裝 Conveyor（Conveyor 22+）
# Windows: winget install Hydraulic.Conveyor
# macOS:   brew install --cask conveyor
# Linux:   https://hydraulic.dev/downloads/

# 首次打包需同意授權
CONVEYOR_AGREE_TO_LICENSE=1 conveyor make windows-msix --overwrite   # macOS/Linux
$env:CONVEYOR_AGREE_TO_LICENSE="1"; conveyor make windows-msix --overwrite  # Windows PowerShell

# 建置所有平台
conveyor make site
```

輸出：
- `output/jpeg2pdf-ofd-cli-3.0.0.x64.msix` (Windows)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip` (macOS Intel)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip` (macOS ARM)
- `output/brian-shih-jpeg2pdf-ofd-cli_3.0.0_amd64.deb` (Linux DEB)

> **注意：** Windows MSIX 若簽章不受信任，可將 `.msix` 改名為 `.zip` 後解壓使用。
> 若僅修改 Java 程式碼而未改動依賴，可直接複製 JAR 到部署目錄的 `app/` 資料夾覆蓋，無需每次重新打包。

---

## 文件列表

- **README.md** - 本文件
- **conveyor.conf** - Conveyor 配置
- **CONVEYOR-GUIDE.md** - 完整 Conveyor 指南
- **JSON-CONFIG-GUIDE.md** - JSON 配置指南
- **searchable_method.md** - Searchable PDF/OFD 完整產生方法
- **SEARCHABLE_OFD_NOTES.md** - 技術筆記
- **TEXTLAYER-CONFIG-GUIDE.md** - textLayer 配置指南
- **CHINA-LINUX-GUIDE.md** - 中國國產 Linux 構建指南 (新)
- **build-china-linux.sh** - Linux/macOS 構建腳本 (新)
- **build-china-linux.ps1** - Windows PowerShell 構建腳本 (新)

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

**跨平台支援：**
- Windows
- macOS (Intel + ARM)
- Linux (Debian + RPM)

**打包選項：**
- Conveyor（推薦） ⭐⭐⭐⭐⭐
- jpackage（僅 Windows）
- JAR（需要 Java）

---

**GitHub：** https://github.com/brianshih04/jpeg2pdf-ofd-conveyor
