     1|# repack-into-zip.ps1
     2|# Takes Conveyor's output ZIP, renames the launcher exe cleanly, adds a start.bat launcher, and creates a clean portable ZIP.
     3|# Usage: powershell -File repack-into-zip.ps1
     4|
     5|$ErrorActionPreference = "Stop"
     6|
     7|$conveyorOutputDir = Join-Path $PSScriptRoot "output"
     8|$distDir = Join-Path $PSScriptRoot "dist-test"
     9|
    10|if (-not (Test-Path $conveyorOutputDir)) {
    11|    Write-Error "Conveyor output directory not found: $conveyorOutputDir`nRun 'conveyor make windows-zip' first."
    12|    exit 1
    13|}
    14|
    15|# Find the latest Conveyor-generated ZIP
    16|$conveyorZip = Get-ChildItem -Path $conveyorOutputDir -Filter "*.zip" |
    17|    Sort-Object LastWriteTime -Descending |
    18|    Select-Object -First 1
    19|
    20|if (-not $conveyorZip) {
    21|    Write-Error "No ZIP found in $conveyorOutputDir"
    22|    exit 1
    23|}
    24|
    25|Write-Host "Source ZIP : $($conveyorZip.FullName)"
    26|Write-Host "Extracting..."
    27|
    28|$extractDir = Join-Path $env:TEMP "conveyor-repack-$([guid]::NewGuid().ToString('N'))"
    29|if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }
    30|New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    31|
    32|Expand-Archive -Path $conveyorZip.FullName -DestinationPath $extractDir -Force
    33|
    34|# Find the launcher exe inside bin/
    35|$binDir = Join-Path $extractDir "bin"
    36|$launcherExe = Get-ChildItem -Path $binDir -Filter "*.exe" |
    37|    Where-Object { $_.Name -notmatch "^(conveyor|update)" } |
    38|    Select-Object -First 1
    39|
    40|if (-not $launcherExe) {
    41|    Write-Error "Launcher exe not found in $binDir"
    42|    Remove-Item $extractDir -Recurse -Force
    43|    exit 1
    44|}
    45|
    46|$exeRawName = $launcherExe.Name
    47|# Conveyor uses display-name for the exe, which may contain %20 (URL-encoded space)
    48|# Rename to a clean filename
    49|$exeCleanName = "JPEG2PDF-OFD-OCR.exe"
    50|Write-Host "Found launcher: $exeRawName -> $exeCleanName"
    51|
    52|# Rename the exe in-place within bin/
    53|Rename-Item -Path $launcherExe.FullName -NewName $exeCleanName -Force
    54|
    55|# Create start.bat at root level
    56|$startBatContent = @"
    57|@echo off
    58|cd /d "%~dp0bin"
    59|start "" "$exeCleanName"
    60|"@
    61|$startBatPath = Join-Path $extractDir "start.bat"
    62|Set-Content -Path $startBatPath -Value $startBatContent -Encoding ASCII
    63|Write-Host "Created start.bat at root level"
    64|
    65|# Build output ZIP name: use fsname + version from conveyor.conf
    66|$outputZipName = "JPEG2PDF-OFD-OCR-v3.0.0-windows-x64.zip"
    67|$outputZipPath = Join-Path $conveyorOutputDir $outputZipName
    68|
    69|if (Test-Path $outputZipPath) { Remove-Item $outputZipPath -Force }
    70|
    71|Write-Host "Creating portable ZIP: $outputZipName"
    72|Compress-Archive -Path (Join-Path $extractDir "*") -DestinationPath $outputZipPath -CompressionLevel Optimal
    73|
    74|Write-Host "Done! Output: $outputZipPath"
    75|
    76|# Also copy to dist-test for quick testing
    77|if (Test-Path $distDir) { Remove-Item $distDir -Recurse -Force }
    78|Copy-Item $extractDir -Destination $distDir -Recurse -Force
    79|Write-Host "Extracted copy saved to: $distDir"
    80|
    81|# Cleanup temp
    82|Remove-Item $extractDir -Recurse -Force
    83|