#!/system/bin/sh
# Run as root on the device if you ever need an emergency rollback.
cmd overlay disable --user 0 dev.hyperstatus.overlay.systemui 2>/dev/null || true
cmd overlay disable --user 0 dev.hyperstatus.overlay.framework 2>/dev/null || true
pm uninstall dev.hyperstatus.overlay.systemui 2>/dev/null || true
pm uninstall dev.hyperstatus.overlay.framework 2>/dev/null || true
am force-stop com.android.systemui 2>/dev/null || true
rm -rf /data/local/tmp/hyperstatus /data/local/tmp/hyperstatus_aapt2 2>/dev/null || true
echo "HyperStatus overlays disabled/removed."
