# æ§‹å»º No Spring Boot ?ˆæœ¬??jpackage ?¯åŸ·è¡Œæ?

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  JPEG2PDF-OFD No Spring Boot"
Write-Host "  jpackage Builder"
Write-Host "========================================"
Write-Host ""

$projectPath = "D:\Projects\jpeg2pdf-ofd-jpackage"

Set-Location $projectPath

# 1. æ§‹å»º JAR
Write-Host "[1/4] æ§‹å»º JAR..."
Write-Host ""

mvn clean package -DskipTests 2>&1 | Out-Null

if ($LASTExitCode -ne 0) {
    Write-Host "  ??æ§‹å»ºå¤±æ?"
    exit 1
}

Write-Host "  ??JAR æ§‹å»º?å?"

# 2. æº–å? jpackage è¼¸å…¥?®é?
Write-Host ""
Write-Host "[2/4] æº–å? jpackage è¼¸å…¥..."

if (Test-Path "jpackage-input") {
    Remove-Item -Recurse -Force "jpackage-input"
}

New-Item -ItemType Directory -Path "jpackage-input" -Force | Out-Null

# è¤‡è£½ JAR
Copy-Item "target\jpeg2pdf-ofd-jpackage-3.0.0-jar-with-dependencies.jar" "jpackage-input\" -Force

Write-Host "  ??è¼¸å…¥?®é?å·²æ???

# 3. ä½¿ç”¨ jpackage ?“å?
Write-Host ""
Write-Host "[3/4] jpackage ?“å?ï¼ˆé€™é?è¦?2-3 ?†é?ï¼?.."
Write-Host ""

jpackage `
  --name "jpeg2pdf-ofd-jpackage" `
  --input jpackage-input `
  --main-jar jpeg2pdf-ofd-jpackage-3.0.0-jar-with-dependencies.jar `
  --main-class com.ocr.nospring.Main `
  --type app-image `
  --dest dist-jpackage `
  --java-options "-Xmx2G" `
  --win-console `
  --app-version "3.0.0" `
  --description "JPEG OCR to Searchable PDF/OFD - No Spring Boot Edition" `
  --vendor "Brian Shih" 2>&1 | Out-Null

if ($LASTExitCode -ne 0) {
    Write-Host "  ??jpackage ?“å?å¤±æ?"
    exit 1
}

Write-Host "  ??jpackage ?“å??å?"

# 4. æ·»å??ç½®?‡ä»¶?Œè…³??Write-Host ""
Write-Host "[4/4] æ·»å??ç½®?‡ä»¶..."

# ?ç½®?‡ä»¶
$config = @{
    input = @{
        folder = "C:/OCR/Watch"
        pattern = "*.jpg"
    }
    output = @{
        folder = "C:/OCR/Output"
        format = "all"
    }
    ocr = @{
        language = "chinese_cht"
    }
}

$config | ConvertTo-Json -Depth 3 | Set-Content -Path "dist-jpackage\jpeg2pdf-ofd-jpackage\config.json" -Encoding UTF8

# ?‹è??³æœ¬
$runScript = @'
@echo off
chcp 65001 >nul
echo ========================================
echo   JPEG2PDF-OFD No Spring Boot v3.0.0
echo   jpackage Edition
echo ========================================
echo.
if "%1"=="" (
    echo Usage: run.bat config.json
    echo.
    pause
    exit /b 1
)

"%~dp0\jpeg2pdf-ofd-jpackage.exe" %1

if errorlevel 1 (
    echo.
    echo Execution failed
    pause
    exit /b 1
)
'@

$runScript | Out-File -FilePath "dist-jpackage\jpeg2pdf-ofd-jpackage\run.bat" -Encoding ASCII

# README
$readme = @'
# JPEG2PDF-OFD No Spring Boot (jpackage Edition)

## ?¹é?

- ???¡é? Java å®‰è?
- ????Spring Boot æ¡†æ¶
- ??å®Œæ•´?Ÿèƒ½ï¼ˆOCR + PDF + OFDï¼?- ???¨ç?è³‡æ?å¤¾ï??…å« runtimeï¼?
## ä½¿ç”¨?¹æ?

```cmd
run.bat config.json
```

?–ç›´?¥é?è¡Œï?

```cmd
jpeg2pdf-ofd-jpackage.exe config.json
```

## ?ç½®

ç·¨è¼¯ `config.json`ï¼?
```json
{
  "input": {
    "folder": "C:/OCR/Watch",
    "pattern": "*.jpg"
  },
  "output": {
    "folder": "C:/OCR/Output",
    "format": "all"
  },
  "ocr": {
    "language": "chinese_cht"
  }
}
```

## ?¯æ??¼å?

- PDF - ?¯æ?ç´?PDF
- OFD - ?¯æ?ç´?OFD
- TXT - ç´”æ???
## ?ˆæœ¬

- Version: 3.0.0
- Framework: None (Pure Java SE)
- Packaging: jpackage (includes runtime)
'@

Set-Content -Path "dist-jpackage\jpeg2pdf-ofd-jpackage\README.md" -Encoding UTF8

Write-Host "  ???ç½®?‡ä»¶å·²æ·»??

# 5. é¡¯ç¤ºçµæ?
Write-Host ""
Write-Host "========================================"
Write-Host "  æ§‹å»ºå®Œæ?ï¼?
Write-Host "========================================"
Write-Host ""

# è¨ˆç?å¤§å?
$folderSize = (Get-ChildItem "dist-jpackage\jpeg2pdf-ofd-jpackage" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
$folderSizeMB = [math]::Round($folderSize, 2)

Write-Host "ä½ç½®ï¼?
Write-Host "  $projectPath\dist-jpackage\jpeg2pdf-ofd-jpackage\"
Write-Host ""
Write-Host "è³‡æ?å¤¾å¤§å°ï?"
Write-Host "  $folderSizeMB MB"
Write-Host ""

# é¡¯ç¤º?‡ä»¶
Write-Host "?‡ä»¶?—è¡¨ï¼?
Get-ChildItem "dist-jpackage\jpeg2pdf-ofd-jpackage" | Select-Object Name, Length | Format-Table -AutoSize

Write-Host ""
Write-Host "========================================"
Write-Host "  ä½¿ç”¨?¹æ?"
Write-Host "========================================"
Write-Host ""
Write-Host "cd dist-jpackage\jpeg2pdf-ofd-jpackage"
Write-Host "run.bat config.json"
Write-Host ""
Write-Host "========================================"
Write-Host "  å°æ?"
Write-Host "========================================"
Write-Host ""
Write-Host "| ?ˆæœ¬ | å¤§å? | Java | Spring | ?®æ?ä»?|"
Write-Host "|------|------|------|--------|--------|"
Write-Host "| jpackage (No Spring) | ~$folderSizeMB MB | ??| ??| ??|"
Write-Host "| jpackage (Spring) | 212 MB | ??| ??| ??|"
Write-Host "| JAR (No Spring) | 52 MB | ??| ??| ??|"
Write-Host ""
Write-Host "========================================"
Write-Host "  ?¨è–¦"
Write-Host "========================================"
Write-Host ""
Write-Host "????Java ?°å? ??jpackage ?ˆæœ¬"
Write-Host "????Java ?°å? ??JAR ?ˆæœ¬ï¼ˆæ›´å°ï?"
Write-Host ""

pause
