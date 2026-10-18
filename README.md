# S Pen Cursor Fix

Fixes the stylus hover pointer on LineageOS and AOSP tablets (Android 16 / LineageOS 23.x). Replaces the desktop mouse arrow with a density-scaled circular dot while keeping mouse, trackpad, and touch input completely stock.

## Architecture

1. **IDC Overlay Module (`spen_idc_overlay-v1.0.0.zip`)**
   - KernelSU / Magisk module that installs `sec_e-pen.idc` with `touch.deviceType = touchScreen`.
   - Prevents the stylus from being classified as an external relative mouse (`AINPUT_SOURCE_MOUSE`), routing events directly to `StylusPointerController` and eliminating cursor flashes on hover entry and touch contact.

2. **LSPosed Module (`SPenCursorFix.apk`)**
   - Hooks `InputManagerService.setPointerIcon` in `system_server`.
   - Replaces the generic pointer for the stylus with a customizable dot icon while respecting drawing canvas `TYPE_NULL` requests.
   - Built with Jetpack Compose (Material 3 Expressive) with live in-app hover preview.

## Installation

1. Install `SPenCursorFix-v1.0.0-release.apk`.
2. In LSPosed, enable the module and set the scope to **System Framework** (`android`).
3. Flash `spen_idc_overlay-v1.0.0.zip` via KernelSU or Magisk.
4. Reboot the device.

## Configuration

Settings can be changed from the app:

| Setting | Default | Description |
|---|---|---|
| Enable S Pen Cursor Fix | `true` | Master toggle for the Xposed hook |
| Hover Dot Color | `#B6E5EA` | Integrated color picker + presets |
| Hover Dot Size | `4.0 dp` | Slider (2.0 to 10.0 dp) |
| Target Device Name | `sec_e-pen` | Input device name match filter |
| Verbose Logging | `false` | Logcat debug diagnostics |

## Building

Prerequisites: JDK 17+, Android SDK (API 36).

```powershell
# Windows
pwsh -File ./scripts/build.ps1
```

```bash
# Linux / macOS
./scripts/build.sh
```

Build outputs are saved to `out/`:
- `SPenCursorFix-v1.0.0-release.apk`
- `spen_idc_overlay-v1.0.0.zip`
- `SPenCursorFix-v1.0.0-Complete-Bundle.zip`
