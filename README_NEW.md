# JPEG2PDF-OFD OCR CLI

**跨平台 OCR 工具：將 JPEG 圖片轉換為可搜尋的 PDF/OFD 文件**

[![GitHub](https://img.shields.io/badge/GitHub-brianshih04%2Fjpeg2pdf--ofd--conveyor-blue)](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange)](https://adoptium.net/)

---

## Features

- Cross-platform support: Windows, macOS (Intel/ARM), Linux
- No Java installation required: Self-contained runtime via Conveyor
- 80+ OCR languages: Traditional Chinese, Simplified Chinese, English, Japanese, Korean, etc.
- Multiple output formats: PDF, OFD (China National Standard), TXT
- Single-page/Multi-page mode: Flexible output options
- Auto-update: Built-in update mechanism (Conveyor)
- Pure Java SE: No Spring Boot dependency, lightweight and fast
- Searchable PDF/OFD: Character-by-character positioning algorithm for precise text layer alignment

---

## Version Comparison

| Version | Size | Requirements | Platforms | Auto-update | Recommendation |
|---------|------|--------------|-----------|-------------|----------------|
| **Conveyor** | **~78 MB** | **No Java needed** | **4 platforms** | Yes | **5 stars** |
| **JAR** | **52 MB** | **Java 17+** | All platforms | No | 3 stars |
| **jpackage** | **181 MB** | **No Java needed** | Windows only | No | 2 stars |

---

## Quick Start

### Windows

#### Method 1: Download MSIX (Recommended)

1. Download: [jpeg2pdf-ofd-cli-3.0.0.x64.msix](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor/releases)
2. Double-click to install
3. Run in PowerShell:
   ```powershell
   jpeg2pdf-ofd config.json
   ```

#### Method 2: PowerShell One-line Install (Auto-update)

```powershell
iex (irm https://brianshih04.github.io/jpeg2pdf-ofd-conveyor/install.ps1)
```

### macOS

```bash
# Download appropriate version
# Intel Mac: jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip
# Apple Silicon: jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip

unzip jpeg2pdf-ofd-cli-3.0.0-mac-*.zip
./jpeg2pdf-ofd-cli config.json
```

### Linux

```bash
# DEB (Ubuntu/Debian)
sudo dpkg -i brian-shih-jpeg2pdf-ofd-cli_3.0.0_amd64.deb

# TAR.GZ (Generic)
tar xzf jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz
./jpeg2pdf-ofd-cli config.json
```

---

## Documentation

- **[JSON Configuration Guide](JSON-CONFIG-GUIDE.md)** - All JSON configuration options
- **[Searchable OFD Generation Method](searchable_method.md)** - How to generate searchable OFD documents
- **[Technical Notes](SEARCHABLE_OFD_NOTES.md)** - Searchable OFD implementation details
- **[textLayer Configuration Guide](TEXTLAYER-CONFIG-GUIDE.md)** - How to configure text layer color and transparency

---

## Configuration

### Complete Configuration Example

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
  "textLayer": {
    "color": "white",
    "opacity": 0.0001
  }
}
```

### Configuration Parameters

#### input Configuration

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `folder` | String | Yes | - | Input image folder path |
| `file` | String | No | - | Single file path |
| `pattern` | String | No | `*.jpg` | File filter pattern |
| `extensions` | Array | No | `["jpg", "jpeg", "png"]` | Supported extensions |

#### output Configuration

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `folder` | String | Yes | - | Output folder path |
| `formats` | Array | No | `["pdf"]` | Output formats: `"pdf"`, `"ofd"`, `"txt"` |
| `multiPage` | Boolean | No | `false` | Merge into multi-page document |

**formats Options:**
- `["pdf"]` - PDF only
- `["ofd"]` - OFD only (China National Standard)
- `["txt"]` - Plain text only
- `["pdf", "ofd"]` - PDF + OFD
- `["pdf", "ofd", "txt"]` - All formats

#### ocr Configuration

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `language` | String | No | `chinese_cht` | OCR language |
| `useGpu` | Boolean | No | `false` | Use GPU acceleration |
| `cpuThreads` | Integer | No | `4` | CPU thread count |

**Supported Languages (80+):**
- `chinese_cht` - Traditional Chinese (default)
- `ch` - Simplified Chinese
- `en` - English
- `japan` - Japanese
- `korean` - Korean
- And 75+ more languages...

#### textLayer Configuration (New Feature)

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `color` | String | No | `"white"` | Text layer color name |
| `red` | Integer | No | `255` | RGB red value (0-255) |
| `green` | Integer | No | `255` | RGB green value (0-255) |
| `blue` | Integer | No | `255` | RGB blue value (0-255) |
| `opacity` | Double | No | `0.0001` | Opacity (0.0 - 1.0) |

**Supported Color Names:**
- `"white"` - White (default, production)
- `"debug"` - Debug mode (red, opaque)
- `"red"` - Red
- `"black"` - Black
- `"blue"` - Blue
- `"green"` - Green

**Debug Mode:**
```json
{
  "textLayer": {
    "color": "debug"  // Red + opaque, easy to observe text positioning
  }
}
```

**Production Mode:**
```json
{
  "textLayer": {
    "color": "white",
    "opacity": 0.0001  // Very low opacity, searchable but almost invisible
  }
}
```

---

## Usage Examples

### Example 1: Traditional Chinese Multi-page Document

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

**Output:**
- 1 multi-page PDF (all pages merged)
- 1 multi-page OFD (all pages merged)
- 1 TXT file (all text extracted)

### Example 2: English Document (Single-page Mode)

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

**Output:**
- One PDF file per image

### Example 3: Debug Mode

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

**Effect:**
- Red opaque text layer
- Easy to observe text positioning accuracy
- Suitable for development and testing

---

## Test Results

**Test Input:**
- 1 JPEG image
- OCR Language: Traditional Chinese
- Configuration: Debug mode (red text layer)

**Test Output:**
```
Sample_20260324_093624.pdf (2.69 MB)
Sample_20260324_090839.ofd (2.64 MB)

Processing time: ~30 seconds
OCR detected: 52 text blocks
Text layer positioning: Precise alignment
WPS search: Searchable
```

---

## Build from Source

### Prerequisites

- JDK 17+
- Maven 3.6+
- Conveyor (for cross-platform packaging)

### Build JAR

```bash
mvn clean package
```

Output: `target/jpeg2pdf-ofd-nospring-3.0.0-jar-with-dependencies.jar`

### Build Conveyor Cross-platform Packages

```bash
# Install Conveyor
# Windows: choco install conveyor
# macOS: brew install --cask conveyor
# Linux: https://www.hydraulic.software/download

# Build all platforms
conveyor make site
```

Output:
- `output/jpeg2pdf-ofd-cli-3.0.0.x64.msix` (Windows)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip` (macOS Intel)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip` (macOS ARM)
- `output/jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz` (Linux)

---

## Files

- **README.md** - This file
- **conveyor.conf** - Conveyor configuration
- **CONVEYOR-GUIDE.md** - Complete Conveyor guide
- **JSON-CONFIG-GUIDE.md** - JSON configuration guide
- **searchable_method.md** - Searchable PDF/OFD generation method
- **SEARCHABLE_OFD_NOTES.md** - Technical notes
- **TEXTLAYER-CONFIG-GUIDE.md** - textLayer configuration guide

---

## Summary

**Complete Features:**
- OCR recognition (80+ languages)
- Searchable PDF generation (PDFBox 2.0.29)
- Searchable OFD generation (ofdrw 2.3.8)
- TXT export
- Single-page mode
- Multi-page mode
- Character-by-character positioning algorithm (precise text layer alignment)
- Custom text layer color and transparency

**Cross-platform Support:**
- Windows
- macOS (Intel + ARM)
- Linux (Debian + RPM)

**Packaging Options:**
- Conveyor (recommended) - 5 stars
- jpackage (Windows only)
- JAR (requires Java)

---

**GitHub:** https://github.com/brianshih04/jpeg2pdf-ofd-conveyor
