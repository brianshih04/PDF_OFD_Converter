# 開發者指南 (Developer Guide)

本文件協助新開發者從原始碼建置、開發與打包 JPEG2PDF-OFD-OCR 專案。

---

## 1. 環境需求 (Prerequisites)

| 工具 | 版本 | 用途 | 安裝方式 |
|------|------|------|---------|
| **JDK 21** (Azul Zulu FX) | 21+ | 編譯與打包 | [Azul 下載](https://www.azul.com/downloads/?version=java-21-lts&os=windows&architecture=x86_64&package=jdk-fx) |
| **Maven** | 3.9+ | 建置工具 | [Maven 下載](https://maven.apache.org/download.cgi) |
| **Conveyor** | 22.0+ | 應用程式打包 | `choco install conveyor` 或手動下載 |
| **Git** | 最新版 | 版本控制 | `winget install Git.Git` |
| **Tesseract OCR** | 5.x (選用) | 測試 Tesseract 語言 | [GitHub Releases](https://github.com/tesseract-ocr/tesseract/releases) |
| **IDE** | - | 開發環境 | IntelliJ IDEA (推薦) / VS Code |

> **主要開發平台**：Windows 10/11 x64
>
> **注意**：pom.xml 中 `java.version` 設為 17，但 Conveyor 打包使用 JDK 21 (Azul Zulu FX)。日常開發使用 JDK 17 或 21 皆可。

---

## 2. 安裝步驟 (Installation Steps)

### 2.1 JDK 21 安裝

```powershell
# 方式 1：winget 安裝 Azul Zulu FX 21
winget install Azul.Zulu.FX.21.JDK

# 方式 2：手動下載
# 前往 https://www.azul.com/downloads/?version=java-21-lts&os=windows&architecture=x86_64&package=jdk-fx
```

設定 `JAVA_HOME` 環境變數：

```powershell
# PowerShell（永久生效）
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Zulu\zulu-21-fx-jdk", "User")
$env:Path += ";$env:JAVA_HOME\bin"

# 驗證
java -version   # 應顯示 21.x.x
javac -version
```

### 2.2 Maven 安裝

```powershell
# 方式 1：winget
winget install Apache.Maven

# 方式 2：Chocolatey
choco install maven

# 驗證
mvn -version   # 應顯示 3.9.x
```

### 2.3 Conveyor 安裝

**Windows**：

```powershell
# 方式 1：Chocolatey（推薦）
choco install conveyor

# 方式 2：winget
winget install Hydraulic.Conveyor

# 方式 3：手動下載
# 前往 https://www.hydraulic.software/download

# 驗證
conveyor --version
```

**macOS**：

```bash
# 使用 Homebrew
brew install --cask conveyor
```

**Linux**：

```bash
# 下載並安裝
wget https://downloads.hydraulic.dev/conveyor/head/Conveyor%20Head.tar
tar xf Conveyor\ Head.tar
sudo ./conveyor/bin/install-conveyor.sh
```

### 2.4 Tesseract OCR 安裝（選用）

僅測試泰文、俄文、阿拉伯文等 Tesseract 語言時需要。

```powershell
# 安裝
winget install UB-Mannheim.TesseractOCR

# 設定 TESSDATA_PREFIX（tessdata 所在目錄）
[System.Environment]::SetEnvironmentVariable("TESSDATA_PREFIX", "C:\Program Files\Tesseract-OCR\tessdata", "User")

# 驗證
tesseract --version
tesseract --list-langs
```

---

## 3. 字體設定 (Font Setup)

程式使用字體來渲染 PDF/OFD 中的文字層。

### 3.1 推薦字體

| 字體 | 覆蓋範圍 | 大小 | 格式 | 推薦度 |
|------|---------|------|------|--------|
| **GoNotoKurrent** | 80+ 文字系統（含 CJK） | ~15.5 MB | TTF | **最推薦**（一個搞定） |
| Noto Sans CJK TC/SC/JP/KR | 各自語言 | 各 ~16 MB | OTF/TTF | 依語言分開安裝 |

#### 萬用字體推薦：GoNotoKurrent

處理非 CJK 語言（泰文、阿拉伯文、希臘文、印地文、西里爾語系等）時，Noto Sans CJK 無法涵蓋這些文字。強烈推薦使用 **GoNotoKurrent**：

- **檔案**：`GoNotoKurrent-Regular.ttf`（約 15.5 MB）
- **覆蓋**：80+ 種現代文字系統（拉丁、CJK、泰文、阿拉伯文、希臘文、印地文、西里爾語系等）
- **格式**：TTF ✅（PDFBox 支援）
- **授權**：SIL OFL 1.1（免費商用）
- **下載**：[GitHub Releases](https://github.com/satbyy/go-noto-universal/releases)

```json
{
  "font": {
    "path": "C:/Fonts/GoNotoKurrent-Regular.ttf"
  }
}
```

> 一個字體搞定所有語言，特別適合需要處理多國文件的場景。

### 3.2 字體安裝

**Windows**（開發機已安裝 GoNotoKurrent）：

```powershell
# 方式 1：雙擊 .ttf 檔 → 安裝
# 方式 2：複製到字體目錄
Copy-Item GoNotoKurrent-Regular.ttf C:\Windows\Fonts\
```

**Linux**：

```bash
# 方式 1：安裝系統字體套件
# Ubuntu/Debian
sudo apt-get install fonts-noto-cjk

# CentOS/RHEL
sudo yum install google-noto-sans-cjk-fonts

# 方式 2：手動安裝自訂字體
sudo mkdir -p /usr/share/fonts/truetype/custom
sudo cp GoNotoKurrent-Regular.ttf /usr/share/fonts/truetype/custom/
fc-cache -fv
```

**macOS**：

```bash
# macOS 已內置中文字體，無需額外配置
# 內建字體路徑：
# /System/Library/Fonts/PingFang.ttc
# /System/Library/Fonts/STHeiti Light.ttf

# 安裝自訂字體：雙擊 .ttf 檔 → 點擊「安裝字體」
```

### 3.3 字體偵測機制

程式按以下優先順序選擇字體：

1. `config.json` 中 `font.path` 指定的路徑
2. 根據 OCR 語言自動選擇（`chinese_cht` → NotoSansTC、`ch` → NotoSansSC、`japan` → NotoSansJP、`korean` → NotoSansKR、其他 → Arial）
3. 系統字體回退（Windows: `C:/Windows/Fonts/`、Linux: `/usr/share/fonts/`、macOS: `/System/Library/Fonts/`）
4. JAR 內嵌的 Noto Sans 字體

> **TTC 格式不支援**：PDFBox 不支援 TTC (TrueType Collection)。請使用 TTF 格式。

### 3.4 字體格式支持

| 格式 | 支持狀態 | 說明 |
|------|---------|------|
| **TTF** | ✅ 支持 | TrueType Font（推薦） |
| **OTF** | ✅ 支持 | OpenType Font |
| **TTC** | ❌ 不支持 | TrueType Collection |

> 如遇 `Cannot load font: 'head' table is mandatory` 錯誤，表示字體格式不支援，請改用 TTF 格式。

---

## 4. Tesseract 語言資料 (Tesseract Language Data)

RapidOCR 支援大部分語言，以下語言會自動切換到 Tesseract 引擎，需要額外的 tessdata：

| 語言 | 語言代碼 | 所需 tessdata |
|------|---------|--------------|
| Hebrew 希伯來文 | `he` | `heb.traineddata` |
| Thai 泰文 | `th` | `tha.traineddata` |
| Russian 俄文 | `ru` | `rus.traineddata` |
| Arabic 阿拉伯文 | `ar` | `ara.traineddata` |
| Greek 希臘文 | `el` | `ell.traineddata` |
| Hindi 印地文 | `hi` | `hin.traineddata` |
| Ukrainian 烏克蘭文 | `uk` | `ukr.traineddata` |

### 安裝步驟

```powershell
# 1. 前往 tessdata GitHub 下載 traineddata 檔案
# https://github.com/tesseract-ocr/tessdata/tree/main

# 2. 放入 tessdata 目錄（Windows 預設）
# C:\Program Files\Tesseract-OCR\tessdata\

# 3. 驗證
tesseract --list-langs
```

> **RapidOCR 語言不需 tessdata**：繁中、簡中、英文、日文、韓文等由 RapidOCR 處理，無需安裝 Tesseract。

---

## 5. 專案建置 (Build)

### 5.1 Clone 與建置

```bash
# Clone 專案
git clone https://github.com/brianshih04/PDF_OFD_Converter.git
cd PDF_OFD_Converter

# 建置 JAR（含所有依賴的 fat JAR）
mvn clean package
```

### 5.2 建置輸出

- **JAR 位置**：`target/jpeg2pdf-ofd-nospring-3.0.0.jar`（~82 MB）
- **Main Class**：`com.ocr.nospring.Main`
- **打包方式**：Maven Shade Plugin（所有依賴打包進單一 JAR）
- **JavaFX 除外**：Shade Plugin 排除 JavaFX（由 Conveyor 打包的 JVM 提供）

### 5.3 快速建置（跳過測試）

```bash
mvn clean package -DskipTests
```

---

## 6. 打包流程 (Packaging)

使用 Conveyor 將 JAR 打包為 Windows 便攜版 ZIP（含自包含 JDK + JavaFX）。

### 6.1 完整打包流程

```bash
# Step 1: 建置 JAR
mvn clean package -DskipTests

# Step 2: 驗證 Conveyor 配置
conveyor validate

# Step 3: Conveyor 打包為 Windows ZIP
conveyor make windows-amd64

# Step 4: 重新封裝為便攜版（重新命名 exe、加入 start.bat）
powershell -File repack-into-zip.ps1
```

### 6.2 輸出檔案

| 步驟 | 檔案 | 說明 |
|------|------|------|
| Conveyor 輸出 | `output/jpeg2pdf-ofd-ocr-3.0.0.x64.zip` | 原始 Conveyor ZIP |
| repack 輸出 | `output/JPEG2PDF-OFD-OCR-v0.10-windows-x64.zip` | 最終便攜版 |

### 6.3 本地快速測試

```bash
# repack-into-zip.ps1 會自動解壓到 dist-test/
# 直接執行：
cd dist-test
.\start.bat
```

### 6.4 Conveyor 設定檔

關鍵設定位於 `conveyor.conf`：

```hocon
app {
    display-name = "JPEG2PDF-OFD-OCR v0.10"    # 版本號在此更新
    fsname = "jpeg2pdf-ofd-ocr"
    inputs += "target/jpeg2pdf-ofd-nospring-3.0.0.jar"
    machines = [ windows.amd64 ]                # 目前僅 Windows
}
app.windows {
    sign = false    # 禁用簽章，僅生成 ZIP
}
```

#### 重要參數

| 參數 | 說明 |
|------|------|
| `display-name` | 用戶看到的名稱 |
| `fsname` | 安裝目錄名稱（無空格） |
| `jvm.gui.main-class` | Main Class 完整路徑 |
| `site.base-url` | 自動更新伺服器 URL |
| `jvm.options` | JVM 選項（如 `-Xmx2G`） |

### 6.5 進階 Conveyor 配置

#### JVM 選項

```hocon
app {
    jvm.options = [
        "-Xmx4G",                        // 最大堆內存 4GB
        "-Djava.awt.headless=true",     // 無頭模式
        "-Dfile.encoding=UTF-8"         // 編碼
    ]
}
```

#### 命令行別名

```hocon
app {
    cli {
        jpeg2pdf = ${app.jvm.gui.main-class}
        ocr-tool = ${app.jvm.gui.main-class}
    }
}
```

安裝後，用戶可以輸入：
```bash
jpeg2pdf config.json
ocr-tool config.json
```

#### 數位簽章

目前專案使用自簽名憑證，測試安裝沒問題，但發布給他人會出現安全警告。若需正式簽章：

```hocon
app.windows {
    signing-certificate = "path/to/cert.pfx"
    signing-password = "YOUR_PASSWORD"
}
```

> Windows 需購買 EV Code Signing Certificate；macOS 需 Apple Developer Program。

#### 授權

- **開源專案**：✅ 免費使用 Conveyor
- **商業專案**：需購買商業授權，查詢 https://www.hydraulic.software/pricing

### 6.6 工作目錄注意事項

CLI 工具可能從任何目錄執行，必須確保使用絕對路徑：

```java
// ✅ 正確：使用絕對路徑
Path configPath = Paths.get(configPathString).toAbsolutePath();

// ❌ 錯誤：相對於 jar 位置
Path configPath = Paths.get("config.json");  // 可能找不到
```

### 6.7 Conveyor 快取

首次運行需要下載 JDK，Conveyor 會緩存：

```bash
# 查看緩存
conveyor cache list

# 清理緩存（重新下載）
conveyor cache purge
```

---

## 7. 專案結構 (Project Structure)

```
PDF_OFD_Converter/
├── src/main/java/com/ocr/nospring/    # Java 原始碼
│   ├── Main.java                      # CLI 進入點
│   ├── GuiApp.java                    # JavaFX GUI 進入點
│   ├── Config.java                    # JSON 配置解析
│   ├── OcrService.java                # RapidOCR 服務
│   ├── TesseractOcrService.java       # Tesseract OCR 服務
│   ├── TesseractLanguageHelper.java   # Tesseract 語言偵測工具
│   ├── PdfService.java                # PDF 生成（PDFBox）
│   ├── OfdService.java                # OFD 生成（ofdrw）
│   ├── PdfToImagesService.java        # PDF 轉圖片
│   ├── TextService.java               # 純文字輸出
│   └── ProcessingService.java         # 處理流程與進度回呼
├── src/main/resources/                # 資源檔案（字體、logback 設定）
├── dist/                              # 範例配置檔
├── dist-test/                         # 本地快速測試目錄
├── output/                            # Conveyor 打包輸出
├── target/                            # Maven 建置輸出
├── conveyor.conf                      # Conveyor 打包設定
├── repack-into-zip.ps1                # 便攜版重新封裝腳本
├── pom.xml                            # Maven 專案設定
└── CLAUDE.md                          # 開發規範（開發者必讀）
```

---

## 8. 核心模組說明 (Core Modules)

| 檔案 | 說明 |
|------|------|
| **Main.java** | CLI 進入點。解析命令列參數（config.json 路徑或 `--gui`），支援 perPage/multiPage 兩種輸出模式 |
| **GuiApp.java** | JavaFX GUI 應用程式。使用 WebView 載入前端 UI，提供圖形化操作介面 |
| **Config.java** | JSON 配置解析類。讀取並驗證 `config.json` 中的所有設定項（input、output、ocr、font 等） |
| **OcrService.java** | RapidOCR 服務。使用 ONNX Runtime 執行 OCR，支援 80+ 種語言（CJK、拉丁語系等） |
| **TesseractOcrService.java** | Tesseract OCR 服務。處理 RapidOCR 不支援的語言（泰文、希伯來文、阿拉伯文等） |
| **TesseractLanguageHelper.java** | Tesseract 語言偵測工具。根據語言代碼判斷是否使用 Tesseract 引擎及對應的 tessdata 名稱 |
| **PdfService.java** | PDF 生成服務。使用 PDFBox 2.0.29 產生可搜索 PDF，採用逐字符定位算法精確對齊文字層 |
| **OfdService.java** | OFD 生成服務。使用 ofdrw 2.3.8 產生可搜索 OFD（中國國家標準格式） |
| **PdfToImagesService.java** | PDF 轉圖片服務。將 PDF 各頁渲染為圖片 |
| **TextService.java** | 純文字輸出服務。將 OCR 結果匯出為 TXT 檔案 |
| **ProcessingService.java** | 處理流程服務。提取處理邏輯並支援進度回呼（Progress Callback） |

### 主要依賴

| 依賴 | 版本 | 用途 |
|------|------|------|
| PDFBox | 2.0.29 | PDF 產生與操作 |
| ofdrw | 2.3.8 | OFD 格式產生 |
| RapidOCR-Java | 0.0.7 | OCR 引擎（ONNX） |
| Tess4j | 5.13.0 | Tesseract OCR 橋接 |
| OpenCC4j | 1.14.0 | 簡繁中文轉換 |
| Jackson | 2.15.3 | JSON 解析 |
| SLF4J + Logback | 2.0.9 / 1.4.11 | 日誌框架 |
| JavaFX | 21.0.2 | GUI 介面（由 Conveyor 提供） |

---

## 9. 測試 (Testing)

### 9.1 CLI 測試

```bash
# 使用範例配置檔
java -jar target/jpeg2pdf-ofd-nospring-3.0.0.jar dist/config-test.json

# 指定自訂配置
java -jar target/jpeg2pdf-ofd-nospring-3.0.0.jar path/to/your-config.json
```

### 9.2 GUI 測試

```bash
# 方式 1：命令列啟動
java -jar target/jpeg2pdf-ofd-nospring-3.0.0.jar --gui

# 方式 2：使用 dist-test/ 快速測試
cd dist-test
.\start.bat
```

### 9.3 Debug 模式

在 `config.json` 中設定 debug 文字層，方便驗證 OCR 定位是否準確：

```json
{
  "textLayer": {
    "color": "debug"
  }
}
```

此設定會將文字層渲染為紅色不透明，方便在 PDF/OFD 上觀察文字位置。

### 9.4 日誌

程式使用 SLF4J + Logback 輸出日誌。日誌設定位於 `src/main/resources/logback.xml`。

---

## 10. 版本控制 (Version Control)

### 10.1 版本號管理

| 版本類型 | 位置 | 目前值 | 說明 |
|---------|------|--------|------|
| **Build 版本** | `conveyor.conf` → `display-name` | `v0.10` | 每次打包 +0.01 |
| **Maven 版本** | `pom.xml` → `<version>` | `3.0.0` | Java 套件版本 |
| **JAR 名稱** | `conveyor.conf` → `inputs` | `jpeg2pdf-ofd-nospring-3.0.0.jar` | 需與 Maven 版本一致 |

### 10.2 分支策略

- **主要分支**：`main`
- 所有開發直接在 `main` 分支進行

### 10.3 Commit 規範

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
feat: 新增功能
fix: 修復錯誤
refactor: 重構
docs: 文件更新
chore: 雜務（建置、依賴等）
```

---

## 附錄 A：GitHub Pages 發布

Conveyor 的自動更新功能依賴靜態網頁託管，可使用 GitHub Pages（免費）：

```bash
# 1. 創建 gh-pages 分支
git checkout -b gh-pages

# 2. 複製 output/ 內容
cp -r output/* .

# 3. 提交並推送
git add .
git commit -m "Add Conveyor output"
git push origin gh-pages
```

然後前往 GitHub 倉庫設定 → Pages → Source → 選擇 `gh-pages` 分支。

> **其他託管選項**：AWS S3、Netlify、Vercel。

---

## 附錄 B：常見問題

### Conveyor 相關

**Q1: Conveyor 找不到 JAR？**

```bash
mvn clean package
ls target/*.jar
```

**Q2: 首次生成速度慢？**

Conveyor 首次運行需下載 JDK，後續會使用緩存。見 [6.7 節](#67-conveyor-快取)。

**Q3: Windows 安裝失敗（SmartScreen 阻擋）？**

```powershell
# 臨時關閉 SmartScreen（僅測試用）
Set-MpPreference -EnableControlledFolderAccess Disabled
```

**Q4: macOS Gatekeeper 阻擋？**

```bash
xattr -cr jpeg2pdf-ofd-cli.app
```

**Q5: Linux 套件安裝問題？**

```bash
# DEB 套件
sudo dpkg -i jpeg2pdf-ofd-cli_*.deb
sudo apt-get install -f

# RPM 套件
sudo rpm -i jpeg2pdf-ofd-cli-*.rpm
```

### 字體相關

**Q6: 某些中文字符無法顯示？**

字體不包含該字符。解決方案：
1. 使用 GoNotoKurrent（覆蓋 80+ 文字系統）
2. 使用支持完整 CJK 的字體（如 Noto Sans CJK）
3. 檢查字體是否正確加載（查看日誌中的警告）

**Q7: TTC 格式字體不支援？**

PDFBox 目前不完全支持 TTC 格式。請使用 TTF 格式的字體。

---

## 快速參考

```bash
# 完整開發流程
git clone https://github.com/brianshih04/PDF_OFD_Converter.git
cd PDF_OFD_Converter
mvn clean package
java -jar target/jpeg2pdf-ofd-nospring-3.0.0.jar --gui

# 完整打包流程
mvn clean package -DskipTests
conveyor make windows-amd64
powershell -File repack-into-zip.ps1
```

---

**更新時間**：2026-04-01
