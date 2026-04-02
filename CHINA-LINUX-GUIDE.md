# 構建中國國產 Linux 版本指南

本指南說明如何為中國國產 Linux 發行版構建 JPEG2PDF-OFD-OCR。

---

## 支持的中國國產 Linux 發行版

| 發行版 | 架構 | 包格式 | 備註 |
|--------|------|--------|------|
| **統信 UOS** | x86_64, aarch64 | DEB | 基於 Deepin |
| **深度 Deepin** | x86_64 | DEB | 友好的桌面系統 |
| **華為 openEuler** | x86_64, aarch64 | RPM | 企業級作業系統 |
| **銀河麒麟** | x86_64, aarch64 | RPM | 國防作業系統 |
| **中標麒麟** | x86_64 | RPM | 商用作業系統 |
| **華為鯤鵬** | aarch64 | DEB/RPM | ARM64 伺服器 |

---

## 前置要求

### 1. 安裝 Conveyor

**Windows:**
```powershell
choco install conveyor
```

**macOS:**
```bash
brew install --cask conveyor
```

**Linux:**
```bash
# 下載並安裝
wget https://downloads.hydraulic.dev/conveyor/download/conveyor-headless.tar
tar xvf conveyor-headless.tar
sudo mv conveyor /usr/local/bin/
```

### 2. 構建 JAR

```bash
mvn clean package
```

---

## 構建方式

### 方式 1：構建所有平台（包含國產 Linux）

```bash
conveyor make site
```

**輸出文件：**
```
output/
├── JPEG2PDF-OFD-OCR-v0.10-windows-x64.zip       # Windows (便攜版)
├── jpeg2pdf-ofd-cli-0.20-linux-amd64.deb       # Linux x86_64 DEB (UOS, Deepin, openEuler)
├── jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm       # Linux x86_64 RPM (Kylin)
├── jpeg2pdf-ofd-cli-0.20-linux-aarch64.deb     # Linux ARM64 DEB (華為鯤鵬)
└── jpeg2pdf-ofd-cli-0.20-linux-aarch64.rpm     # Linux ARM64 RPM (銀河麒麟)
```

### 方式 2：僅構建國產 Linux 版本

```bash
# 僅構建 x86_64 DEB (UOS, Deepin, openEuler)
conveyor make app.linux.amd64.deb

# 僅構建 x86_64 RPM (銀河麒麟, 中標麒麟)
conveyor make app.linux.amd64.rpm

# 僅構建 ARM64 DEB (華為鯤鵬 DEB 版)
conveyor make app.linux.aarch64.deb

# 僅構建 ARM64 RPM (華為鯤鵬 RPM 版)
conveyor make app.linux.aarch64.rpm
```

---

## 安裝方式

### 統信 UOS / Deepin (DEB)

```bash
# 下載 DEB 包
wget https://github.com/brianshih04/PDF_OFD_Converter/releases/download/v0.20/jpeg2pdf-ofd-cli-0.20-linux-amd64.deb

# 安裝
sudo dpkg -i jpeg2pdf-ofd-cli-0.20-linux-amd64.deb

# 如果有依賴問題，修復依賴
sudo apt-get install -f

# 使用
jpeg2pdf-ofd config.json
```

### 銀河麒麟 / 中標麒麟 (RPM)

```bash
# 下載 RPM 包
wget https://github.com/brianshih04/PDF_OFD_Converter/releases/download/v0.20/jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm

# 安裝
sudo rpm -ivh jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm

# 或使用 yum/dnf
sudo yum install jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm

# 使用
jpeg2pdf-ofd config.json
```

### 華為 openEuler (RPM)

```bash
# 下載 RPM 包
wget https://github.com/brianshih04/PDF_OFD_Converter/releases/download/v0.20/jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm

# 安裝
sudo dnf install jpeg2pdf-ofd-cli-0.20-linux-amd64.rpm

# 使用
jpeg2pdf-ofd config.json
```

### 華為鯤鵬 (ARM64)

```bash
# 下載 ARM64 DEB 包
wget https://github.com/brianshih04/PDF_OFD_Converter/releases/download/v0.20/jpeg2pdf-ofd-cli-0.20-linux-aarch64.deb

# 安裝
sudo dpkg -i jpeg2pdf-ofd-cli-0.20-linux-aarch64.deb
sudo apt-get install -f

# 使用
jpeg2pdf-ofd config.json
```

---

## 配置示例

### 繁體中文 OCR (默認)

```json
{
  "input": {
    "folder": "/home/user/documents",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "/home/user/output",
    "formats": ["pdf", "ofd", "txt"],
    "multiPage": true
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

### 簡體中文 OCR

```json
{
  "input": {
    "folder": "/home/user/documents",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "/home/user/output",
    "formats": ["pdf", "ofd", "txt"]
  },
  "ocr": {
    "language": "ch"
  }
}
```

---

## 已測試平台

| 平台 | 版本 | 測試狀態 |
|------|------|----------|
| 統信 UOS 20 | x86_64 | ✅ 通過 |
| Deepin 20.9 | x86_64 | ✅ 通過 |
| openEuler 22.03 | x86_64 | ✅ 通過 |
| openEuler 22.03 | aarch64 | ✅ 通過 |
| 銀河麒麟 V10 | x86_64 | ⏳ 待測試 |
| 銀河麒麟 V10 | aarch64 | ⏳ 待測試 |

---

## 性能優化建議

### 華為鯤鵬 (ARM64)

```bash
# 使用更多 CPU 線程
export OMP_NUM_THREADS=8
jpeg2pdf-ofd config.json
```

### 一般優化

```json
{
  "ocr": {
    "cpuThreads": 8,
    "useGpu": false
  }
}
```

---

## 常見問題

### Q1: DEB 安裝失敗，提示依賴問題？

```bash
sudo apt-get update
sudo apt-get install -f
```

### Q2: RPM 安裝失敗，提示缺少庫？

```bash
# openEuler / Kylin
sudo dnf install java-17-openjdk-headless

# 或安裝依賴
sudo dnf install glibc
```

### Q3: 運行時提示權限問題？

```bash
# 確保可執行權限
chmod +x /usr/bin/jpeg2pdf-ofd
```

### Q4: 如何在國產 CPU 上優化性能？

```json
{
  "ocr": {
    "cpuThreads": 16,  // 調整為 CPU 核心數
    "useGpu": false    // 國產 GPU 支持有限
  }
}
```

---

## 技術支持

**GitHub Issues:** https://github.com/brianshih04/PDF_OFD_Converter/issues

**文檔:**
- [README.md](README.md) - 完整文檔
- [JSON-CONFIG-GUIDE.md](JSON-CONFIG-GUIDE.md) - JSON 配置指南
- [SEARCHABLE_OFD_NOTES.md](SEARCHABLE_OFD_NOTES.md) - 可搜索 PDF/OFD 技術筆記

---

**最後更新:** 2026-04-01
