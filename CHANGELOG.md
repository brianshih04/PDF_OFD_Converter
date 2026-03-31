# Changelog

## [v0.10] - 2026-04-01

### Changed
- **打包方式從 MSIX 改為 ZIP 便攜版**：解壓即用，無需安裝
- **應用程式更名**：`JPEG2PDF-OFD OCR` → `JPEG2PDF-OFD-OCR`
- **UI 標題顯示版本號**：`conveyor.conf` display-name 設為 `JPEG2PDF-OFD-OCR v0.10`
- **啟動方式改為 `start.bat`**：位於 ZIP 根目錄，自動 cd 進 bin/ 啟動 exe
- **預設目錄選擇器**：開啟使用者家目錄，並在 session 間記住上次使用的路徑
- **Conveyor 配置調整**：
  - `machines` 僅保留 `windows.amd64`
  - `sign = false`（移除自簽憑證設定）
  - 使用 `azul.conf` + `from-jmods.conf` 提供 JavaFX 支援
- **輸出檔名**：`JPEG2PDF-OFD-OCR-v0.10-windows-x64.zip`

### Added
- **`repack-into-zip.ps1`**：重新封裝腳本，重命名 exe、加入 start.bat、產出乾淨便攜 ZIP
- **`start.bat`**：根目錄啟動腳本（由 repack 腳本自動產生）

---

## [3.0.0] - 2026-03-31

### Added
- **JavaFX GUI 模式**：內建 WebView 介面，支援資料夾選擇、格式設定、語言切換、即時處理
- **三語介面**：支援繁體中文 (zh-TW)、簡體中文 (zh-CN)、英文 (en)
- **Conveyor 打包**：使用 Azul Zulu FX 21 打包為 Windows 獨立 desktop app（無需安裝 Java）
- **i18n 支援**：GUI 語言可在執行時動態切換

### Changed
- Fat JAR 排除 JavaFX 依賴（由 Conveyor 打包的 JVM 模組提供），JAR 從 130MB 瘦身至 82MB
- 打包工具從 maven-assembly-plugin 切換為 maven-shade-plugin
- `conveyor.conf` 升級：JDK 17 → Azul Zulu FX 21，移除 headless 模式，CLI 改為 GUI 入口
- JavaBridge 改為類別欄位持有強引用，修復 GC 導致的 JS bridge 失效

### Fixed
- OcrService 臨時檔清理
- OfdService 字體配置改為讀取 Config
- 單頁/多頁處理邏輯重構（淨減 146 行）
- JavaBridge GC 導致語言切換後 `javaApp undefined`
