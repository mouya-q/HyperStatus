#!/system/bin/sh
# HyperStatus Lite emergency disable. Only touches HyperStatus Lite overlays/module files.
set -u
PKGS="dev.hyperstatus.iconifylite.systemui dev.hyperstatus.iconifylite.framework"
for p in $PKGS; do
  cmd overlay disable --user 0 "$p" >/dev/null 2>&1 || true
done
rm -f /system/product/overlay/dev.hyperstatus.iconifylite.systemui.apk
rm -f /system/product/overlay/dev.hyperstatus.iconifylite.framework.apk
rm -f /data/adb/modules/HyperStatusLite/system/product/overlay/dev.hyperstatus.iconifylite.systemui.apk
rm -f /data/adb/modules/HyperStatusLite/system/product/overlay/dev.hyperstatus.iconifylite.framework.apk
killall com.android.systemui >/dev/null 2>&1 || true
