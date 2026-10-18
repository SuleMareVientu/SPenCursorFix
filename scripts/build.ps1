# ==============================================================================
# S Pen Cursor Fix - Build & Package Script (v1.0.0)
# PowerShell Native Version
# ==============================================================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$OutDir = Join-Path $RootDir "out"
$IdcDir = Join-Path $RootDir "idc_module"

$Version = "1.0.0"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Building S Pen Cursor Fix v$Version" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

# 1. Run Gradle build
Write-Host "[+] Compiling LSPosed Xposed Module APK..." -ForegroundColor Green
$GradleBat = Join-Path $RootDir "gradlew.bat"
if (-not (Test-Path $GradleBat)) {
    Write-Error "gradlew.bat not found in $RootDir"
}

& cmd /c "$GradleBat assembleDebug assembleRelease"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
}

# 2. Copy APKs
$DebugApk = Join-Path $RootDir "app\build\outputs\apk\debug\app-debug.apk"
$ReleaseApk = Join-Path $RootDir "app\build\outputs\apk\release\app-release.apk"

if (Test-Path $DebugApk) {
    $DestDebug = Join-Path $OutDir "SPenCursorFix-v$Version-debug.apk"
    Copy-Item -Path $DebugApk -Destination $DestDebug -Force
    Write-Host "[+] Copied debug APK: $DestDebug" -ForegroundColor Green
}

if (Test-Path $ReleaseApk) {
    $DestRelease = Join-Path $OutDir "SPenCursorFix-v$Version-release.apk"
    Copy-Item -Path $ReleaseApk -Destination $DestRelease -Force
    Write-Host "[+] Copied signed release APK: $DestRelease" -ForegroundColor Green
}

# 3. Package KernelSU / Magisk IDC Overlay Module
Write-Host "[+] Packaging KernelSU / Magisk IDC Module..." -ForegroundColor Green
$IdcZip = Join-Path $OutDir "spen_idc_overlay-v$Version.zip"
if (Test-Path $IdcZip) { Remove-Item $IdcZip -Force }

$FilesToZip = @(
    (Join-Path $IdcDir "customize.sh"),
    (Join-Path $IdcDir "module.prop"),
    (Join-Path $IdcDir "system")
)

Compress-Archive -Path $FilesToZip -DestinationPath $IdcZip -Force
Write-Host "[+] Created IDC module: $IdcZip" -ForegroundColor Green

# 4. Generate Complete Bundle
Write-Host "[+] Generating Complete Release Bundle..." -ForegroundColor Green
$BundleTmp = Join-Path $RootDir "build\bundle_tmp"
if (Test-Path $BundleTmp) { Remove-Item $BundleTmp -Recurse -Force }
New-Item -ItemType Directory -Path $BundleTmp | Out-Null

$ReadmeContent = @"
============================================================
S Pen Cursor Fix for LineageOS for Tablets (Android 16 / AOSP)
Version: 1.0.0
============================================================

COMPONENTS:
1. SPenCursorFix-v1.0.0-debug.apk (or release)
   - LSPosed Xposed module that hooks InputManagerService.setPointerIcon.
   - Replaces the generic desktop pointer with a solid, density-scaled
     OneUI-style circular hover dot.
   - Provides an in-app Settings UI to adjust color, radius, and allowlist.

2. spen_idc_overlay-v1.0.0.zip
   - KernelSU / Magisk module that installs /system/usr/idc/sec_e-pen.idc
     and /system/vendor/usr/idc/sec_e-pen.idc with touch.deviceType = touchScreen.
   - Shifts pointer routing in Android 16 from MousePointerController to
     native StylusPointerController, eliminating the initial hover entry and
     touch-down desktop mouse arrow cursor flashes.

INSTALLATION:
1. Install SPenCursorFix-v1.0.0-release.apk (or debug).
2. In LSPosed Manager, enable the module with scope: "System Framework" (android).
3. In KernelSU or Magisk Manager, flash spen_idc_overlay-v1.0.0.zip.
4. Reboot the tablet.
============================================================
"@
Set-Content -Path (Join-Path $BundleTmp "README.txt") -Value $ReadmeContent

if (Test-Path (Join-Path $OutDir "SPenCursorFix-v$Version-debug.apk")) {
    Copy-Item -Path (Join-Path $OutDir "SPenCursorFix-v$Version-debug.apk") -Destination $BundleTmp -Force
}
if (Test-Path (Join-Path $OutDir "SPenCursorFix-v$Version-release.apk")) {
    Copy-Item -Path (Join-Path $OutDir "SPenCursorFix-v$Version-release.apk") -Destination $BundleTmp -Force
}
Copy-Item -Path $IdcZip -Destination $BundleTmp -Force

$BundleZip = Join-Path $OutDir "SPenCursorFix-v$Version-Complete-Bundle.zip"
if (Test-Path $BundleZip) { Remove-Item $BundleZip -Force }
Compress-Archive -Path (Join-Path $BundleTmp "*") -DestinationPath $BundleZip -Force
Remove-Item $BundleTmp -Recurse -Force
Write-Host "[+] Created release bundle: $BundleZip" -ForegroundColor Green

# 5. Checksums
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Artifact Checksums (SHA256)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Get-ChildItem -Path $OutDir -File | ForEach-Object {
    $Hash = (Get-FileHash -Path $_.FullName -Algorithm SHA256).Hash
    Write-Host "$($_.Name) : $Hash"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Build Completed Successfully!" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
