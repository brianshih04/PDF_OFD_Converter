# 構建中國國產 Linux 版本 (PowerShell)
# Build script for Chinese domestic Linux distributions

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  JPEG2PDF-OFD OCR CLI" -ForegroundColor Green
Write-Host "  中國國產 Linux 構建腳本" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查是否已安裝 Conveyor
$conveyorPath = Get-Command conveyor -ErrorAction SilentlyContinue
if (-not $conveyorPath) {
    Write-Host "錯誤：未找到 Conveyor" -ForegroundColor Red
    Write-Host "請先安裝 Conveyor: https://www.hydraulic.software/download" -ForegroundColor Yellow
    exit 1
}

# 構建 JAR
Write-Host "步驟 1: 構建 JAR 文件..." -ForegroundColor Yellow
mvn clean package
if ($LASTEXITCODE -ne 0) {
    Write-Host "錯誤：JAR 構建失敗" -ForegroundColor Red
    exit 1
}
Write-Host "✅ JAR 構建成功" -ForegroundColor Green
Write-Host ""

# 選擇要構建的平台
Write-Host "步驟 2: 選擇要構建的平台" -ForegroundColor Yellow
Write-Host "1) 全部平台（包括 Windows, macOS, Linux）"
Write-Host "2) 僅 Linux x86_64 (統信 UOS, Deepin, openEuler)"
Write-Host "3) 僅 Linux ARM64 (華為鯤鵬, 飛騰)"
Write-Host "4) 僅 Linux DEB 包"
Write-Host "5) 僅 Linux RPM 包"
Write-Host ""
$choice = Read-Host "請選擇 (1-5)"

switch ($choice) {
    "1" {
        Write-Host "構建所有平台..." -ForegroundColor Yellow
        conveyor make site
    }
    "2" {
        Write-Host "構建 Linux x86_64 (DEB + RPM)..." -ForegroundColor Yellow
        conveyor make app.linux.amd64.deb
        conveyor make app.linux.amd64.rpm
    }
    "3" {
        Write-Host "構建 Linux ARM64 (DEB + RPM)..." -ForegroundColor Yellow
        conveyor make app.linux.aarch64.deb
        conveyor make app.linux.aarch64.rpm
    }
    "4" {
        Write-Host "構建 Linux DEB 包..." -ForegroundColor Yellow
        conveyor make app.linux.amd64.deb
        conveyor make app.linux.aarch64.deb
    }
    "5" {
        Write-Host "構建 Linux RPM 包..." -ForegroundColor Yellow
        conveyor make app.linux.amd64.rpm
        conveyor make app.linux.aarch64.rpm
    }
    default {
        Write-Host "無效選擇" -ForegroundColor Red
        exit 1
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "  ✅ 構建成功！" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "輸出文件位於：output\" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "文件列表：" -ForegroundColor Yellow
    Get-ChildItem output | Format-Table Name, Length -AutoSize
    Write-Host ""
    Write-Host "支持的中國國產 Linux：" -ForegroundColor Green
    Write-Host "  - 統信 UOS (DEB)"
    Write-Host "  - 深度 Deepin (DEB)"
    Write-Host "  - 華為 openEuler (RPM)"
    Write-Host "  - 銀河麒麟 (RPM)"
    Write-Host "  - 中標麒麟 (RPM)"
    Write-Host "  - 華為鯤鵬 (DEB/RPM ARM64)"
} else {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Red
    Write-Host "  ❌ 構建失敗" -ForegroundColor Red
    Write-Host "=========================================" -ForegroundColor Red
    Write-Host "請檢查錯誤信息並重試" -ForegroundColor Yellow
    exit 1
}
