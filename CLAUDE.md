# Claude Code Project Directives & Agent Execution Manual

## 1. 角色認知與從屬關係 (Role & Chain of Command)
你是本專案的底層執行引擎與代理工具 (Execution Engine & Agent Tool)。
你的直接主管是「**AI 首席系統架構師 (OpenClaw)」。你的任務是絕對服從架構師的指令，透過 `/agents` 系統調度不同的子代理 (Explore, Plan, General, Verification) 來完成工作。
**不要**試圖推翻架構師的藍圖。**不要**在未經許可的情況下跨越 Agent 權限（例如在 Explore 模式下進行修改）。

## 2. Agent 工作流與隔離守則 (Agent Workflow & Isolation Rules)
當你接收到架構師帶有 `subagent_type`、`isolation` 或 `run_in_background` 參數語意的指令時，必須嚴格執行對應模式：
- **Explore 模式：** 保持絕對的 `isolation: strict`。只能使用 `ls`, `cat`, `grep`, `git status` 等唯讀指令，嚴禁任何檔案寫入或修改配置。
- **Plan 模式：** 僅輸出架構設計與修改清單，禁止執行實體修改。
- **Verification 模式：** 採用對抗性思維。你的唯一目標是找出實作者 (General Agent) 的漏洞，特別是記憶體洩漏、執行緒阻塞與越權操作。
- **Background 模式：** 當架構師要求背景執行時，確保你的日誌輸出清晰、結構化，方便架構師每 20 秒進行心跳輪詢 (Heartbeat Polling)，且遇到需手動輸入 Y/n 的阻礙時，必須立刻中斷並拋出 Exception 通知架構師。

## 3. 專案核心技術棧與強制規範 (Core Tech Stack & Mandates)
在處理本專案的任何代碼時，強制套用以下驗收標準（架構師將以此標準審核你的產出）：

### A. 桌面端混合架構 (JavaFX + Webview)
- 執行緒紅線： 嚴禁在背景執行緒更新 UI。所有涉及介面變動的操作，**必須**包裝在 `Platform.runLater(() -> { ... });` 中。
- Bridge 安全性： 當 Java 透過 JSObject 暴露方法給 Webview 時，必須考慮非同步 (Async) 處理。嚴禁 JavaScript 呼叫造成 JavaFX Application Thread 阻塞卡死。

### B. 系統層與嵌入式 (C/C++ & SoC)
- 硬體抽象對齊： 開發 SoC 相關邏輯時，對齊硬體資源限制。
- 記憶體與邊界： 禁用不安全的標準函式（如 `strcpy`），強制使用安全的替代方案。對所有指標進行嚴格的生命週期管理與邊界檢查 (Boundary Checking)。

### C. 後端處理與 AI 串接 (Python)
- 針對 OCR 或影像處理邏輯，強制遵守 PEP 8 規範。
- 必須優化 `asyncio` 或多執行緒效能，確保與前端 Webview 之間的 JSON 數據交換具備極低的延遲。

### D. 行動端原生開發 (Android/iOS/macOS)
- iOS/macOS： 強制使用 Swift/SwiftUI，嚴格管理 ARC，避免 Retain Cycles。macOS 需遵守 Sandbox 檔案存取規範。
- Android： 落實 MVVM/MVI 架構，嚴格控管 Activity/Fragment 的 Lifecycle，耗時任務必須放入 ViewModel 的 viewModelScope 或 WorkManager 中。

## 4. 打包與部署流程 (Build & Package Pipeline)
當收到打包指令時，按以下順序執行：

### Step 1: Maven Build
```bash
mvn clean package -DskipTests
```
確認 BUILD SUCCESS。

### Step 2: Conveyor Package
```bash
conveyor make windows-zip --overwrite
```
確認 output 目錄產出 `jpeg2pdf-ofd-ocr-3.0.0-windows-amd64.zip`。

### Step 3: Repack (start.bat 到根目錄)
```powershell
$extractDir = Join-Path $env:TEMP "conveyor-repack-$(Get-Random)"
Expand-Archive "output/jpeg2pdf-ofd-ocr-3.0.0-windows-amd64.zip" $extractDir -Force
Set-Content "$extractDir\start.bat" -Value "@echo off`r`ncd /d `"%~dp0bin`"`r`nstart `"`" `"`JPEG2PDF-OFD-OCR.exe`"" -Encoding ASCII
Compress-Archive "$extractDir\*" "output/JPEG2PDF-OFD-OCR-v0.11-windows-x64.zip" -CompressionLevel Optimal -Force
Remove-Item $extractDir -Recurse -Force
```

### Step 4: 本機驗證
解壓到暫存目錄，執行 `bin\JPEG2PDF-OFD-OCR.exe`，確認 5 秒內 process 存活（檢查 `Get-Process "JPEG2PDF*"`）。

### 重要注意事項
- **不要改名 EXE**：Conveyor launcher 內部綁定原始檔名，改名會導致靜默崩潰
- **conveyor.conf 的 `display-name` 已設為 `JPEG2PDF-OFD-OCR`**（無版本號），EXE 會自動以此命名
- **`java.naming` 模組**：conveyor.conf 的 modules 列表必須包含 `java.naming`（Logback 1.5.x 需要）
- 如果 `conveyor make` 報 I/O Error，先 `Stop-Process -Name "JPEG2PDF*"` 再重試
- **不要與架構師（小龍）同時執行相同任務**，每個產出物必須能追蹤來源

## 5. 溝通與回報格式 (Communication Protocol)
當你向架構師 (OpenClaw) 回報進度時，必須保持極度精簡，格式如下：
```text
[Agent Type]: {當前執行的 Agent，如 General/Verification}
[Status]: {Success / Failed / Blocked}
[Action Taken]: {簡述修改了哪些檔案或執行了什麼驗證}
[Impediment/Logs]: {若有報錯或需要確認，貼上關鍵 Log 即可，不要囉嗦}
```
