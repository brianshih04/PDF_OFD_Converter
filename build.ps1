# æ§‹å»º No Spring Boot ?ˆæœ¬

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  JPEG2PDF-OFD No Spring Boot Builder"
Write-Host "========================================"
Write-Host ""

$projectPath = "D:\Projects\jpeg2pdf-ofd-jpackage"

# 1. ç·¨è­¯
Write-Host "[1/3] ç·¨è­¯ No Spring Boot ?ˆæœ¬..."
Write-Host ""

Set-Location $projectPath

mvn clean package -DskipTests 2>&1 | Out-Null

if ($LASTExitCode -ne 0) {
    Write-Host "  ??ç·¨è­¯å¤±æ?"
    Write-Host ""
    Write-Host "?¥ç??¯èª¤ï¼?
    mvn clean compile 2>&1 | Select-String "ERROR", "error" -Context 1,1
    exit 1
}

Write-Host "  ??ç·¨è­¯?å?"

# 2. ?µå»º?†ç™¼
Write-Host ""
Write-Host "[2/3] ?µå»º?†ç™¼..."

Copy-Item "target\jpeg2pdf-ofd-jpackage-3.0.0-jar-with-dependencies.jar" "dist\jpeg2pdf-ofd-jpackage.jar" -Force
Copy-Item "config.json" "dist\config.json" -Force

Write-Host "  ???†ç™¼å·²å‰µå»?

# 3. ?µå»º?‹è??³æœ¬
Write-Host ""
Write-Host "[3/3] ?µå»º?‹è??³æœ¬..."

$runScript = @'
@echo off
chcp 65001 >nul
echo ========================================
echo   JPEG2PDF-OFD No Spring Boot v3.0.0
echo   Pure Java SE Edition
echo ========================================
echo.
if "%1"=="" (
    echo Usage: run.bat config.json
    echo.
    pause
    exit /b 1
)

java -Xmx2G -jar jpeg2pdf-ofd-jpackage.jar %1

if errorlevel 1 (
    echo.
    echo Execution failed
    pause
    exit /b 1
)
'@

$runScript | Out-File -FilePath "dist\run.bat" -Encoding ASCII

Write-Host "  ???‹è??³æœ¬å·²å‰µå»?

# 4. é¡¯ç¤ºçµæ?
Write-Host ""
Write-Host "========================================"
Write-Host "  æ§‹å»ºå®Œæ?ï¼?
Write-Host "========================================"
Write-Host ""

# è¨ˆç?å¤§å?
$jar = Get-Item "dist\jpeg2pdf-ofd-jpackage.jar"
$jarSize = [math]::Round($jar.Length / 1MB, 2)

Write-Host "ä½ç½®ï¼?
Write-Host "  $projectPath\dist\"
Write-Host ""
Write-Host "JAR å¤§å?ï¼?
Write-Host "  jpeg2pdf-ofd-jpackage.jar: $jarSize MB"
Write-Host ""
Write-Host "========================================"
Write-Host "  ä½¿ç”¨?¹æ?"
Write-Host "========================================"
Write-Host ""
Write-Host "cd dist"
Write-Host "run.bat config.json"
Write-Host ""
Write-Host "?–ï?"
Write-Host ""
Write-Host "java -Xmx2G -jar jpeg2pdf-ofd-jpackage.jar config.json"
Write-Host ""
Write-Host "========================================"
Write-Host "  ?ªå‹¢"
Write-Host "========================================"
Write-Host ""
Write-Host "????Spring Bootï¼ˆæ›´å°ã€æ›´å¿«ï?"
Write-Host "???¡æ??¶ä?è³´ï??´ç°¡?®ï?"
Write-Host "??ç´?Java SEï¼ˆæ›´?¯é?ï¼?
Write-Host ""

pause
