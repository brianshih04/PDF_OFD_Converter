# Build standalone desktop app using Conveyor

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  JPEG2PDF-OFD Conveyor Builder"
Write-Host "========================================"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# 1. Maven build
Write-Host "[1/3] Maven clean package..."
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
mvn clean package -q 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Maven build failed!"
    exit 1
}
Write-Host "  Done"

# 2. Conveyor build
Write-Host "[2/3] Conveyor make windows-zip..."
conveyor make windows-zip --overwrite 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Conveyor build failed!"
    exit 1
}
Write-Host "  Done"

# 3. Inject start.bat into ZIP root
Write-Host "[3/3] Injecting start.bat into ZIP..."
$zipFile = Get-Item "output\jpeg2pdf-ofd-ocr-*-windows-amd64.zip" | Select-Object -Last 1
if ($zipFile) {
    Compress-Archive -Path "start.bat" -DestinationPath $zipFile.FullName -Update
    Write-Host "  Done"
} else {
    Write-Host "  Warning: ZIP not found, skipping start.bat injection"
}

Write-Host ""
$size = [math]::Round($zipFile.Length / 1MB, 1)
Write-Host "Output: $($zipFile.FullName) ($size MB)"
