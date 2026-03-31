# repack-into-zip.ps1
# Takes Conveyor's output ZIP, copies the launcher exe to root level, and creates a clean portable ZIP.
# Usage: powershell -File repack-into-zip.ps1

$ErrorActionPreference = "Stop"

$conveyorOutputDir = Join-Path $PSScriptRoot "output"
$distDir = Join-Path $PSScriptRoot "dist-test"

if (-not (Test-Path $conveyorOutputDir)) {
    Write-Error "Conveyor output directory not found: $conveyorOutputDir`nRun 'conveyor make windows-zip' first."
    exit 1
}

# Find the latest Conveyor-generated ZIP
$conveyorZip = Get-ChildItem -Path $conveyorOutputDir -Filter "*.zip" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $conveyorZip) {
    Write-Error "No ZIP found in $conveyorOutputDir"
    exit 1
}

Write-Host "Source ZIP : $($conveyorZip.FullName)"
Write-Host "Extracting..."

$extractDir = Join-Path $env:TEMP "conveyor-repack-$([guid]::NewGuid().ToString('N'))"
if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }
New-Item -ItemType Directory -Path $extractDir -Force | Out-Null

Expand-Archive -Path $conveyorZip.FullName -DestinationPath $extractDir -Force

# Find the launcher exe inside bin/
$binDir = Join-Path $extractDir "bin"
$launcherExe = Get-ChildItem -Path $binDir -Filter "*.exe" |
    Where-Object { $_.Name -notmatch "^(conveyor|update)" } |
    Select-Object -First 1

if (-not $launcherExe) {
    Write-Error "Launcher exe not found in $binDir"
    Remove-Item $extractDir -Recurse -Force
    exit 1
}

$exeRawName = $launcherExe.Name
# Conveyor uses display-name for the exe, which may contain %20 (URL-encoded space)
# Rename to a clean filename for the root-level copy
$exeCleanName = "JPEG2PDF-OFD-OCR.exe"
Write-Host "Found launcher: $exeRawName -> $exeCleanName"

# Copy the exe to root of extracted dir with a clean name
Copy-Item $launcherExe.FullName -Destination (Join-Path $extractDir $exeCleanName) -Force

# Build output ZIP name: use fsname + version from conveyor.conf
$outputZipName = "JPEG2PDF-OFD-OCR-v0.10-windows-x64.zip"
$outputZipPath = Join-Path $conveyorOutputDir $outputZipName

if (Test-Path $outputZipPath) { Remove-Item $outputZipPath -Force }

Write-Host "Creating portable ZIP: $outputZipName"
Compress-Archive -Path (Join-Path $extractDir "*") -DestinationPath $outputZipPath -CompressionLevel Optimal

Write-Host "Done! Output: $outputZipPath"

# Also copy to dist-test for quick testing
if (Test-Path $distDir) { Remove-Item $distDir -Recurse -Force }
Copy-Item $extractDir -Destination $distDir -Recurse -Force
Write-Host "Extracted copy saved to: $distDir"

# Cleanup temp
Remove-Item $extractDir -Recurse -Force
