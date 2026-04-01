# Conveyor 打包指南

## 📦 使用 Conveyor 打包 JPEG2PDF-OFD-OCR

這個項目使用 [Conveyor](https://www.hydraulic.software/) 進行打包，目前僅生成 Windows x64 便攜版 ZIP。

---

## 🛠 安裝 Conveyor

### Windows

#### 方法 1：使用 Chocolatey（推薦）

```powershell
# 安裝 Chocolatey（如果尚未安裝）
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 安裝 Conveyor
choco install conveyor
```

#### 方法 2：手動下載

1. 前往：https://www.hydraulic.software/download
2. 下載 Windows 安裝檔（.msi 或 .exe）
3. 執行安裝程式
4. 重新打開終端機

### macOS

```bash
# 使用 Homebrew
brew install --cask conveyor
```

### Linux

```bash
# 下載並安裝
wget https://downloads.hydraulic.dev/conveyor/head/Conveyor%20Head.tar
tar xf Conveyor\ Head.tar
sudo ./conveyor/bin/install-conveyor.sh
```

---

## 📋 打包步驟

### 1️⃣ 構建 JAR

```bash
# 在項目根目錄執行
mvn clean package
```

確認 `target/` 目錄下生成了 `jpeg2pdf-ofd-nospring-3.0.0-jar-with-dependencies.jar`。

---

### 2️⃣ 驗證 Conveyor 配置

```bash
# 檢查配置文件
cat conveyor.conf

# 驗證配置
conveyor validate
```

---

### 3️⃣ 生成安裝檔

```bash
# 生成所有平台的安裝檔 + 網站
conveyor make site
```

這條指令會：
- 下載並壓縮 JDK（Windows、macOS、Linux）
- 創建原生啟動器
- 生成便攜版 ZIP
- 啟用自動更新功能（如配置 site.base-url）

---

### 4️⃣ 查看輸出

```bash
# 查看生成的文件
ls output/

# Windows: .zip (便攜版)
```

---

## 🚀 發布到 GitHub Pages

### 1️⃣ 創建 GitHub Pages

```bash
# 創建 gh-pages 分支
git checkout -b gh-pages

# 複製 output/ 內容
cp -r output/* .

# 提交
git add .
git commit -m "Add Conveyor output"

# 推送
git push origin gh-pages
```

### 2️⃣ 啟用 GitHub Pages

1. 前往 GitHub 倉庫設定
2. Pages → Source → 選擇 `gh-pages` 分支
3. 保存

您的下載頁面將在：
```
https://brianshih04.github.io/jpeg2pdf-ofd-conveyor/
```

---

## ⚙️ 配置文件說明

### `conveyor.conf` 主要配置

```hocon
app {
    // 應用程式名稱
    display-name = "JPEG2PDF-OFD-OCR"
    
    // 文件系統名稱（小寫+連字號）
    fsname = "jpeg2pdf-ofd-cli"
    
    // Main Class
    jvm.gui.main-class = "com.ocr.nospring.Main"
    
    // 自動更新網站
    site.base-url = "https://brianshih04.github.io/PDF_OFD_Converter"
}
```

### 重要參數

| 參數 | 說明 |
|------|------|
| `display-name` | 用戶看到的名稱 |
| `fsname` | 安裝目錄名稱（無空格） |
| `jvm.gui.main-class` | Main Class 完整路徑 |
| `site.base-url` | 自動更新伺服器 URL |
| `jvm.options` | JVM 選項（如 `-Xmx2G`） |

---

## 📝 自定義配置

### 添加命令行別名

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

### 添加 JVM 選項

```hocon
app {
    jvm.options = [
        "-Xmx4G",                        // 最大堆內存 4GB
        "-Djava.awt.headless=true",     // 無頭模式
        "-Dfile.encoding=UTF-8"         // 編碼
    ]
}
```

### 排除依賴

```hocon
app {
    jvm {
        // 排除不必要的模組
        modules -= "java.desktop"  // CLI 工具可能不需要
    }
}
```

---

## ⚠️ 重要注意事項

### 1. 數位簽章（Code Signing）

**現狀：**
- Conveyor 默認使用自簽名憑證
- 測試安裝沒問題
- 發布給他人會出現安全警告

**解決方案：**
- 購買正式的程式碼簽章憑證
- Windows: EV Code Signing Certificate
- macOS: Apple Developer Program

**配置簽章：**
```hocon
app.windows {
    signing-certificate = "path/to/cert.pfx"
    signing-password = "YOUR_PASSWORD"
}
```

### 2. 靜態網頁託管

**必需：**
- GitHub Pages（免費，推薦）
- AWS S3
- Netlify
- Vercel

**原因：**
- Conveyor 的自動更新功能依賴 `site.base-url`
- 必須將 `output/` 上傳到網頁伺服器

### 3. 工作目錄問題

**注意：**
- CLI 工具可能從任何目錄執行
- 確保 Java 代碼使用絕對路徑
- 不要依賴 `.jar` 檔所在位置

**Java 代碼範例：**
```java
// ✅ 正確：使用絕對路徑
Path configPath = Paths.get(configPathString).toAbsolutePath();

// ❌ 錯誤：相對於 jar 位置
Path configPath = Paths.get("config.json");  // 可能找不到
```

### 4. 授權問題

**開源專案：** ✅ 免費使用

**商業專案：**
- 需要購買商業授權
- 查詢：https://www.hydraulic.software/pricing

---

## 🔧 常見問題

### Q1: Conveyor 找不到 JAR？

**解決：**
```bash
# 確保已構建 JAR
mvn clean package

# 檢查 target/ 目錄
ls target/*.jar
```

### Q2: 生成速度慢？

**原因：**
- 首次運行需要下載 JDK
- Conveyor 會緩存 JDK

**解決：**
```bash
# 查看緩存
conveyor cache list

# 清理緩存（重新下載）
conveyor cache purge
```

### Q3: Windows 安裝失敗？

**可能原因：**
- 缺少簽章
- SmartScreen 阻擋

**解決：**
```powershell
# 臨時關閉 SmartScreen（僅測試用）
Set-MpPreference -EnableControlledFolderAccess Disabled

# 或添加排除項
Add-MpPreference -ExclusionPath "output"
```

### Q4: macOS Gatekeeper 阻擋？

**解決：**
```bash
# 允許運行
xattr -cr jpeg2pdf-ofd-cli.app

# 或在系統偏好設定中允許
```

### Q5: Linux 權限問題？

**解決：**
```bash
# DEB 套件
sudo dpkg -i jpeg2pdf-ofd-cli_*.deb
sudo apt-get install -f  # 修復依賴

# RPM 套件
sudo rpm -i jpeg2pdf-ofd-cli-*.rpm
```

---

## 📚 參考資源

- **Conveyor 官方文檔：** https://conveyor.hydraulic.dev/
- **Maven 配置：** https://conveyor.hydraulic.dev/configs/jvm/maven
- **GitHub Pages 設定：** https://pages.github.com/
- **程式碼簽章：** https://www.hydraulic.software/blog/code-signing

---

## 🎯 快速開始

```bash
# 1. 安裝 Conveyor
choco install conveyor

# 2. 構建項目
mvn clean package

# 3. 打包
conveyor make site

# 4. 測試
cd output
# Windows: 解壓 ZIP 並運行 start.bat
```

---

**更新時間：** 2026-04-01
