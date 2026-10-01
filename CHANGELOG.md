# Changelog

## 0.5.1
- Kept the four requested Iconify resources only.
- Fixed the stale libsu import so the project no longer requires libsu.
- Fixed the missing `Modifier.size` import in the Miuix UI.
- Hardened the root-shell timeout so a stalled `su` process does not block the UI task indefinitely.
- Kept the Iconify-style AAPT2 → zipalign → APK-signing pipeline.

## 0.5.0
- First Iconify Lite standalone root implementation.
