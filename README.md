# HyperStatus Iconify Lite v0.5.1

A minimal standalone root APK derived from the supplied Iconify beta architecture. It keeps only the four requested tweaks:

1. Notch Bar Killer
2. Status bar height
3. Start padding
4. End padding

The resource payloads and value semantics intentionally mirror Iconify's corresponding implementation. The dynamic APK compiler uses the same AAPT2 -> zipalign -> APK signing pipeline; the UI is Miuix 0.9.4.

## Important deployment difference on Android 17 / SDK 37

The supplied Iconify beta tries to `pm install` the generated overlay APK before copying it into the system overlay directory. On the user's Android 17 result this causes PackageManager to reject the overlay because of certificate/targetName validation. This Lite build **does not install the overlay APK as a normal application**. It writes the signed RRO into `/system/product/overlay` (when writable) and also keeps a private systemless copy under `/data/adb/modules/HyperStatusLite/system/product/overlay` for persistence. No `hyperos.rustruntime.*` file is read, modified, replaced, deleted, or copied.

If the current boot cannot discover the new overlay package immediately, the app reports that a reboot is required rather than restarting `system_server`.

## Root

Uses the Android root shell (`su`) directly. No LSPosed/Xposed dependency or scope. No Hilt/Room/DataStore.

## Build

GitHub Actions uses JDK 21, AGP 9.4.1, Kotlin 2.4.20, compileSdk 37, and Miuix 0.9.4.

## License / provenance

The overlay compiler/signing portions are derived from the supplied Iconify beta project. See `LICENSE-ICONIFY.txt`.
