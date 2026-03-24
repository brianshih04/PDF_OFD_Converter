# JSON 配置指南 - textLayer 配置

## 概述

從 `jpeg2pdf-ofd-jpackage-test` 應用過來的 Searchable OFD 優化現已整合到 `jpeg2pdf-ofd-conveyor` 專案。

---

## 新增配置：textLayer

從版本 3.0.0 開始，您可以在 JSON 配置中設定文字層的顏色和透明度。

### 客整配置結構

```json
{
  "input": {
    "folder": "C:/OCR/Input",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/OCR/Output",
    "format": "ofd",
    "multiPage": false
  },
  "ocr": {
    "language": "chinese_cht"
  },
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

### textLayer 配置選項

| 參數 | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `color` | String | 否 | `"white"` | 文字層顏色名稱 |
| `red` | Integer | 否 | `255` | RGB 紅色值 (0-255) |
| `green` | Integer | 否 | `255` | RGB 綠色值 (0-255) |
| `blue` | Integer | 否 | `255` | RGB 藍色值 (0-255) |
| `opacity` | Double | 否 | `0.0001` | 透明度 (0.0 - 1.0) |

### 支持的顏色名稱

| 顏色 | RGB 值 | 用途 |
|------|--------|------|
| `"white"` | (255, 255, 255) | 白色（默認，生產環境） |
| `"debug"` | (255, 0, 0) | **調試模式**：紅色不透明 |
| `"red"` | (255, 0, 0) | 紅色 |
| `"black"` | (0, 0, 0) | 黑色 |
| `"blue"` | (0, 0, 255) | 藍色 |
| `"green"` | (0, 255, 0) | 綠色 |

### 配置示例

#### 生產環境
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```
效果：文字幾乎看不見，但可以被搜索

#### 調試環境
```json
{
  "textLayer": {
    "color": "debug"
  }
}
```
效果：紅色不透明文字，方便觀察定位準確性

#### 自定義 RGB
```json
{
  "textLayer": {
    "red": 128,
    "green": 128,
    "blue": 128,
    "opacity": 0.5
  }
}
```
效果：灰色半透明文字

---

## 測試配置

專案已包含 `config-test-conveyor.json` 測試文件，用於驗證 textLayer 配置是否正常工作。

