# JPEG2PDF-OFD OCR CLI

**Cross-platform OCR tool to convert JPEG images to searchable PDF/OFD documents.**

[![GitHub](https://img.shields.io/badge/GitHub-brianshih04%2Fjpeg2pdf--ofd--conveyor-blue)](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange)](https://adoptium.net/)

---

## Features

- **Cross-platform support**: Windows, macOS (Intel/ARM), Linux
- **Self-contained**: No Java installation required (Conveyor packaged)
- **80+ OCR languages**: Chinese Traditional/Simplified, English, Japanese, Korean, etc.
- **Multiple output formats**: PDF, OFD (China National Standard), TXT
- **Single/Multi-page mode**: Flexible output options
- **Auto-update**: Built-in update mechanism (Conveyor)
- **Pure Java SE**: No Spring Boot dependencies, lightweight and fast

---

## Version Comparison

| Version | Size | Requirements | Platforms | Auto-update | Recommended |
|---------|------|--------------|-----------|-------------|-------------|
| **Conveyor** | ~78 MB | No Java needed | 4 platforms | Yes | **5 stars** |
| **JAR** | 52 MB | Java 17+ | All platforms | No | 4 stars |
| **jpackage** | 181 MB | No Java needed | Windows only | No | 3 stars |

---

## Quick Start

### Windows

**Method 1: Download MSIX (Recommended)**

1. Download: [jpeg2pdf-ofd-cli-3.0.0.x64.msix](https://github.com/brianshih04/jpeg2pdf-ofd-conveyor/releases)
2. Double-click to install
3. Run in PowerShell:
   ```powershell
   jpeg2pdf-ofd config.json
   ```

**Method 2: PowerShell One-liner (Auto-update)**

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

# TAR.GZ (Universal)
tar xzf jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz
./jpeg2pdf-ofd-cli config.json
```

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
| `folder` | String | Yes | - | **Output folder path** |
| `formats` | Array | No | `["pdf"]` | Output formats |
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
- `chinese_cht` - Chinese Traditional (default)
- `ch` - Chinese Simplified
- `en` - English
- `japan` - Japanese
- `korean` - Korean
- And 75+ more...

---

## Usage Examples

### Example 1: Chinese Traditional Multi-Page

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

### Example 2: English Documents (Single-Page)

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
- One PDF per image

### Example 3: Single File Processing

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
  }
}
```

---

## Test Results

**Test Input:**
- 8 JPEG images
- Config: `config-multipage-example.json`

**Test Output:**
```
Success: multipage_20260323_203921.pdf (14.96 MB, 8 pages)
Success: multipage_20260323_203921.ofd (14.60 MB, 8 pages)

Processing time: ~60 seconds
OCR detected: 543 text blocks
```

---

## Bug Fixes Applied

### Issue 1: Configuration Parsing Error (FIXED)

**Problem:** Only PDF generated, OFD and TXT not generated

**Cause:** Configuration used "formats" (plural) but code only checked "format" (singular)

**Fix:**
```java
// Support both "formats" and "format"
Object formats = outputConfig.get("formats");
if (formats == null) {
    formats = outputConfig.get("format"); // Backward compatible
}
```

---

### Issue 2: PDF Font Loading Failure (FIXED)

**Problem:** Warning: Cannot load font from C:/Windows/Fonts/msyh.ttc

**Fix:** Added fallback fonts (Arial, SimHei, SimSun)

---

### Issue 3: PDF Text Rendering Error (FIXED)

**Problem:** Error: Nested beginText() calls are not allowed

**Fix:** Ensured beginText/endText pairing with try-finally

---

### Issue 4: OFD Multi-Page Generation (FIXED)

**Problem:** OFD file generated but size is 0

**Fix:** Configuration parsing fixed, OFD now generates correctly

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

### Build Conveyor Cross-Platform Packages

```bash
# Install Conveyor from https://www.hydraulic.software/download

# Build all platforms
conveyor make site
```

Output:
- `output/jpeg2pdf-ofd-cli-3.0.0.x64.msix` (Windows)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-amd64.zip` (macOS Intel)
- `output/jpeg2pdf-ofd-cli-3.0.0-mac-aarch64.zip` (macOS ARM)
- `output/jpeg2pdf-ofd-cli-3.0.0-linux-amd64.tar.gz` (Linux)

---

## Documentation

- **README.md** - This file
- **conveyor.conf** - Conveyor configuration
- **CONVEYOR-GUIDE.md** - Complete Conveyor guide
- **JSON-CONFIG-GUIDE.md** - JSON configuration guide

---

## Summary

**Complete Features:**
- OCR recognition (80+ languages)
- PDF generation (PDFBox 2.0.29)
- OFD generation (ofdrw 2.3.8)
- TXT export
- Single-page mode
- Multi-page mode

**Cross-Platform Support:**
- Windows
- macOS (Intel + ARM)
- Linux (Debian + RPM)

**Packaging Options:**
- Conveyor (recommended)
- jpackage (Windows only)
- JAR (requires Java)

**All Issues Fixed:**
- Configuration parsing
- PDF fonts
- PDF text rendering
- OFD multi-page generation

**Test Results:**
- 8 images to 1 multi-page PDF + 1 multi-page OFD

---

**GitHub:** https://github.com/brianshih04/jpeg2pdf-ofd-conveyor
