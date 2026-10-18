#!/usr/bin/env bash
# ==============================================================================
# S Pen Cursor Fix - Build & Package Script (v1.0.0)
# Target: LineageOS for Tablets (Android 16 / AOSP)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${ROOT_DIR}/out"
IDC_DIR="${ROOT_DIR}/idc_module"

VERSION="1.0.0"

echo "============================================================"
echo "  Building S Pen Cursor Fix v${VERSION}"
echo "============================================================"

mkdir -p "${OUT_DIR}"

# 1. Detect Gradle Wrapper
if [ -f "${ROOT_DIR}/gradlew" ]; then
    GRADLE_CMD="${ROOT_DIR}/gradlew"
elif [ -f "${ROOT_DIR}/gradlew.bat" ]; then
    GRADLE_CMD="${ROOT_DIR}/gradlew.bat"
else
    echo "[-] Error: gradlew not found in ${ROOT_DIR}"
    exit 1
fi

# 2. Build APK
echo "[+] Compiling LSPosed Xposed Module APK..."
cd "${ROOT_DIR}"
"${GRADLE_CMD}" assembleDebug assembleRelease

# Locate generated APKs
DEBUG_APK="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="${ROOT_DIR}/app/build/outputs/apk/release/app-release.apk"

if [ -f "${DEBUG_APK}" ]; then
    cp -f "${DEBUG_APK}" "${OUT_DIR}/SPenCursorFix-v${VERSION}-debug.apk"
    echo "[+] Copied debug APK to: ${OUT_DIR}/SPenCursorFix-v${VERSION}-debug.apk"
fi

if [ -f "${RELEASE_APK}" ]; then
    cp -f "${RELEASE_APK}" "${OUT_DIR}/SPenCursorFix-v${VERSION}-release.apk"
    echo "[+] Copied signed release APK to: ${OUT_DIR}/SPenCursorFix-v${VERSION}-release.apk"
fi

# 3. Package KernelSU / Magisk IDC Overlay Module
echo "[+] Packaging KernelSU / Magisk IDC Module..."
IDC_ZIP="${OUT_DIR}/spen_idc_overlay-v${VERSION}.zip"
rm -f "${IDC_ZIP}"

cd "${IDC_DIR}"
if command -v zip >/dev/null 2>&1; then
    zip -r -q -9 "${IDC_ZIP}" customize.sh module.prop system/
elif command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -Command "Compress-Archive -Path '${IDC_DIR}\customize.sh', '${IDC_DIR}\module.prop', '${IDC_DIR}\system' -DestinationPath '${IDC_ZIP}' -Force"
else
    echo "[-] Warning: Neither 'zip' nor 'powershell.exe' found. Skipping zip creation."
fi

if [ -f "${IDC_ZIP}" ]; then
    echo "[+] Created IDC module: ${IDC_ZIP}"
fi

# 4. Generate Combined Release Bundle
BUNDLE_DIR="${ROOT_DIR}/build/bundle_tmp"
BUNDLE_ZIP="${OUT_DIR}/SPenCursorFix-v${VERSION}-Complete-Bundle.zip"
rm -rf "${BUNDLE_DIR}" "${BUNDLE_ZIP}"
mkdir -p "${BUNDLE_DIR}"

cat << 'EOF' > "${BUNDLE_DIR}/README.txt"
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
EOF

cp -f "${OUT_DIR}/SPenCursorFix-v${VERSION}-debug.apk" "${BUNDLE_DIR}/" 2>/dev/null || true
if [ -f "${OUT_DIR}/SPenCursorFix-v${VERSION}-release.apk" ]; then
    cp -f "${OUT_DIR}/SPenCursorFix-v${VERSION}-release.apk" "${BUNDLE_DIR}/" 2>/dev/null || true
fi
cp -f "${IDC_ZIP}" "${BUNDLE_DIR}/" 2>/dev/null || true

if command -v zip >/dev/null 2>&1; then
    (cd "${BUNDLE_DIR}" && zip -r -q -9 "${BUNDLE_ZIP}" .)
elif command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -Command "Compress-Archive -Path '${BUNDLE_DIR}\*' -DestinationPath '${BUNDLE_ZIP}' -Force"
fi
rm -rf "${BUNDLE_DIR}"

echo "[+] Created complete release bundle: ${BUNDLE_ZIP}"

# 5. Checksums
echo ""
echo "============================================================"
echo "  Artifact Checksums (SHA256)"
echo "============================================================"
cd "${OUT_DIR}"
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum *
elif command -v certutil.exe >/dev/null 2>&1; then
    for f in *; do
        if [ -f "$f" ]; then
            echo -n "$f: "
            certutil.exe -hashfile "$f" SHA256 | sed -n '2p'
        fi
    done
fi

echo "============================================================"
echo "  Build Completed Successfully!"
echo "============================================================"
