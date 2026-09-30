# HyperStatus

A minimal root-assisted status-bar tuner inspired by the relevant Iconify features, tailored for Xiaomi 10 / HyperOS 4 ports.

## Included
- Notch Bar Killer (framework Cutout resources)
- Status bar height
- Status bar start padding
- Status bar end padding
- Read-only compatibility scan
- One-tap restore that disables/uninstalls only HyperStatus overlays

## Safety model
The runtime engine has a hard allow-list for only `android` and `com.android.systemui`. It does not use LSPosed/Xposed, does not write or delete framework files, and contains an explicit resource-name guard against `rustruntime`.

This is intentionally not an absolute guarantee: OEM resource implementations differ, and a bad overlay can still affect SystemUI. Apply one setting at a time and keep the restore button available.

## Build
Push the repo to GitHub. The included workflow builds a debug APK with JDK 17.

### Device requirement
The included AAPT2 binary is arm64-v8a, matching Xiaomi 10. Other architectures need their own AAPT2 binary.
