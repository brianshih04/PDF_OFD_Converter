# Changelog

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
