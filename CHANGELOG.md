# Changelog

## 0.3.0-miuix

- Rebuilt UI with Miuix Android components.
- Added selectable diagnostics/error output and copy-all action.
- Replaced `assets/Tools/aapt2-arm64-v8a` lookup with an explicit `res/raw/aapt2_arm64_v8a` resource.
- Kept the runtime target allow-list restricted to `android` and `com.android.systemui`.
- Added explicit arm64 ABI verification before executing the bundled AAPT2.
- Kept the existing compile-first / install-later / rollback behavior for overlays.
- Updated GitHub Actions to JDK 21, Kotlin/Compose compiler 2.4.20, AGP 9.4.1 and Gradle 9.7.1.
