# 字體配置指南

JPEG2PDF-OFD 需要字體來渲染 PDF/OFD 中的文字層。

## 自動字體選擇

程序會自動按以下順序選擇字體：

1. **配置文件中指定的字體**（如果提供）
2. **根據 OCR 語言自動選擇**：
   - `chinese_cht` → NotoSansTC（繁體中文）
   - `ch` → NotoSansSC（簡體中文）
   - `japan` → NotoSansJP（日文）
   - `korean` → NotoSansKR（韓文）
   - 其他 → Arial（系統默認）
3. **系統字體回退**（如果上述字體不可用）

---

## 平台特定配置

### Windows

Windows 系統通常已內置中文字體，無需額外配置。

**推薦字體**：
```
C:/Windows/Fonts/kaiu.ttf       # 標楷體（TTF 格式）
C:/Windows/Fonts/msjh.ttc       # 微軟正黑體（TTC 格式，不支持）
```

**配置示例**：
```json
{
  "font": {
    "path": "C:/Windows/Fonts/kaiu.ttf"
  }
}
```

---

### Linux

需要安裝中文字體。

**Ubuntu/Debian**：
```bash
sudo apt-get update
sudo apt-get install fonts-noto-cjk
```

**CentOS/RHEL**：
```bash
sudo yum install google-noto-sans-cjk-fonts
```

**字體路徑**：
```
/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc
```

**配置示例**：
```json
{
  "font": {
    "path": "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
  }
}
```

---

### macOS

macOS 已內置中文字體，無需額外配置。

**字體路徑**：
```
/System/Library/Fonts/PingFang.ttc
/System/Library/Fonts/STHeiti Light.ttf
```

---

## 自定義字體

### 配置方法

在 `config.json` 中添加 `font` 配置：

```json
{
  "input": {
    "folder": "P:/OCR/input",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "P:/OCR/output",
    "format": "all"
  },
  "font": {
    "path": "/path/to/your/font.ttf"
  },
  "textLayer": {
    "color": "red",
    "opacity": 1.0
  }
}
```

### 字體格式支持

| 格式 | 支持狀態 | 說明 |
|------|---------|------|
| **TTF** | ✅ 支持 | TrueType Font |
| **OTF** | ✅ 支持 | OpenType Font |
| **TTC** | ❌ 不支持 | TrueType Collection（暫不支持） |

---

## 推薦字體

### 開源字體（可免費使用）

| 字體 | 語言 | 下載 |
|------|------|------|
| **Noto Sans CJK** | 全語言 | [Google Fonts](https://fonts.google.com/noto) |
| **思源黑體** | 中文 | [GitHub](https://github.com/adobe-fonts/source-han-sans) |
| **思源宋體** | 中文 | [GitHub](https://github.com/adobe-fonts/source-han-serif) |
| **GoNotoKurrent** | **80+ 文字系統** | [GitHub Releases](https://github.com/satbyy/go-noto-universal/releases) |

### 🌍 萬用字體推薦：GoNotoKurrent

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

### 系統內置字體

| 平台 | 繁體中文 | 簡體中文 |
|------|---------|---------|
| **Windows** | 標楷體、微軟正黑體 | 宋體、黑體 |
| **macOS** | 蘋方-繁 | 蘋方-簡 |
| **Linux** | Noto Sans CJK TC | Noto Sans CJK SC |

---

## 常見問題

### Q: 為什麼某些中文字符無法顯示？

**A**: 字體不包含該字符。解決方案：
1. 使用支持完整 CJK 的字體（如 Noto Sans CJK）
2. 檢查字體是否正確加載（查看日誌中的警告）

### Q: TTC 格式字體為什麼不支持？

**A**: PDFBox 目前不完全支持 TTC 格式。請使用 TTF 格式的字體。

### Q: 如何確認字體是否正確加載？

**A**: 查看日誌輸出：
```
Loaded font: C:/Windows/Fonts/kaiu.ttf
```

如果看到錯誤信息：
```
Cannot load font: 'head' table is mandatory
```
表示字體格式不支持。

---

## 字體文件位置

程序會按以下順序查找字體：

1. 配置文件中指定的路徑
2. 系統字體目錄：
   - Windows: `C:/Windows/Fonts/`
   - Linux: `/usr/share/fonts/`
   - macOS: `/System/Library/Fonts/`
3. 內嵌的 Noto Sans 字體（JAR 中自帶）

---

## 進階配置

### 多語言支持

如果您的文檔包含多種語言，建議使用 **Noto Sans CJK**：

```json
{
  "font": {
    "path": "/path/to/NotoSansCJK-Regular.ttc"
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

### 字體回退順序

程序會按以下順序嘗試渲染字符：

1. 配置的字體
2. NotoSansTC/NotoSansSC（根據 OCR 語言）
3. 系統字體
4. 跳過該字符（記錄警告）
