# Conveyor 打包踩坑記錄

## 1. Logback 1.5.x 需要 java.naming 模組
- **現象**：EXE 靜默崩潰，無錯誤訊息
- **原因**：Logback 1.5.x 依賴 `javax.naming.NamingException`，jlinked JDK 預設不含 `java.naming` 模組
- **修法**：`conveyor.conf` 的 `app.jvm.modules` 加入 `"java.naming"`
- **診斷**：用 `conveyor run` 可看到具體錯誤（直接跑 EXE 看不到）

## 2. 不要改名 EXE
- Conveyor launcher 內部綁定原始檔名（來自 `display-name`）
- 後手動改名會導致靜默崩潰
- **修法**：直接在 `conveyor.conf` 設好 `display-name`

## 3. display-name 不要帶版本號
- 帶版本號會生成 `JPEG2PDF-OFD-OCR v0.11.exe`，start.bat 和用戶都要跟著改
- **修法**：`display-name = "JPEG2PDF-OFD-OCR"`，版本號只在 `app.version` 管理

## 4. console = true 留 CMD 視窗
- debug 時設 `console = true` 可看錯誤訊息
- 正式版要改回 `console = false`

## 正確打包流程
1. `mvn clean package -DskipTests`
2. `conveyor make windows-zip --overwrite`
3. Repack：解壓 → start.bat 放根目錄 → 壓回 zip
4. 本機驗證（`conveyor run` 或解壓後直接跑）
