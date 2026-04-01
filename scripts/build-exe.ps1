# 構建 Windows Desktop App (EXE)

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  JPEG2PDF-OFD Windows Desktop Builder"
Write-Host "========================================"
Write-Host ""

$projectPath = "D:\Projects\jpeg2pdf-ofd-conveyor"

Set-Location $projectPath

# 1. 準備 jpackage 輸入
Write-Host "[1/4] 準備 jpackage 輸入..."

if (Test-Path "jpackage-input") {
    Remove-Item -Recurse -Force "jpackage-input"
}

New-Item -ItemType Directory -Path "jpackage-input" -Force | Out-Null

# 複製 JAR
Copy-Item "target\jpeg2pdf-ofd-nospring-3.0.0-jar-with-dependencies.jar" "jpackage-input\" -Force

Write-Host "  ✓ 輸入目錄已準備"

# 2. 創建 dist-exe 目錄
Write-Host ""
Write-Host "[2/4] 準備輸出目錄..."

if (Test-Path "dist-exe") {
    Remove-Item -Recurse -Force "dist-exe"
}

New-Item -ItemType Directory -Path "dist-exe" -Force | Out-Null

Write-Host "  ✓ 輸出目錄已創建"

# 3. 使用 jpackage 打包
Write-Host ""
Write-Host "[3/4] jpackage 打包（需要 2-3 分鐘）..."
Write-Host ""

jpackage `
  --name "JPEG2PDF-OFD" `
  --input jpackage-input `
  --main-jar jpeg2pdf-ofd-nospring-3.0.0-jar-with-dependencies.jar `
  --main-class com.ocr.nospring.Main `
  --type app-image `
  --dest dist-exe `
  --java-options "-Xmx2G" `
  --java-options "-Dfile.encoding=UTF-8" `
  --icon "icon.ico" `  # 如果沒有 icon.ico，請移除此行
  --app-version "3.0.0" `
  --vendor "Brian Shih" `
  --description "JPEG to PDF/OFD Converter with OCR" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --win-per-user-install

if ($LASTExitCode -ne 0) {
    Write-Host "  ✗ jpackage 打包失敗"
    exit 1
}

Write-Host ""
Write-Host "  ✓ jpackage 打包完成"

# 4. 複製配置文件
Write-Host ""
Write-Host "[4/4] 複製配置文件..."

Copy-Item "config.json" "dist-exe\JPEG2PDF-OFD\" -ErrorAction SilentlyContinue
Copy-Item "dist\config-test-*.json" "dist-exe\JPEG2PDF-OFD\" -ErrorAction SilentlyContinue

Write-Host "  ✓ 配置文件已複製"

# 5. 顯示結果
Write-Host ""
Write-Host "========================================"
Write-Host "  構建完成！"
Write-Host "========================================"
Write-Host ""

# 計算大小
$exe = Get-Item "dist-exe\JPEG2PDF-OFD\JPEG2PDF-OFD.exe" -ErrorAction SilentlyContinue
if ($exe) {
    $exeSize = [math]::Round($exe.Length / 1KB, 2)
    Write-Host "EXE: $exeSize KB"
}

# 計算總大小
$totalSize = (Get-ChildItem "dist-exe\JPEG2PDF-OFD" -Recurse | Measure-Object -Property Length -Sum).Sum
$totalSizeMB = [math]::Round($totalSize / 1MB, 2)

Write-Host "總大小: $totalSizeMB MB"
Write-Host ""
Write-Host "位置: $projectPath\dist-exe\JPEG2PDF-OFD\"
Write-Host ""
Write-Host "使用方式:"
Write-Host "  cd dist-exe\JPEG2PDF-OFD"
Write-Host "  .\JPEG2PDF-OFD.exe --config config.json"
Write-Host ""
