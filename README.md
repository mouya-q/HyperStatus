# HyperStatus
Minimal root-assisted status bar tuning for Xiaomi 10 / HyperOS 4 ports.

**Safety-first starter:** this repository builds an Android APK with a compatibility report UI and read-only root diagnostics. It does not install overlays, patch framework files, hook SystemUI, or touch `hyperos.rustruntime.*`. Actual tuning is deliberately gated until the ROM's overlayable resources and behavior are verified.

## Build on GitHub
Push this folder to a repository. GitHub Actions builds a debug APK on push (`.github/workflows/android.yml`).

## Why not ship automatic changes yet?
Resource names and overlay policies vary by ROM/port. Blindly changing status bar dimensions can cause SystemUI layout failures. First install the APK, grant root when prompted by the diagnostics screen, and collect the report. Use that report to implement only confirmed resources.

## Project
- `app`: Kotlin Android app, Material-style restrained UI
- `RootDiagnostics`: read-only `getprop`, overlay list and resource-file discovery
- No LSPosed dependency, no Xposed hooks, no runtime-library modification
