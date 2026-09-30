package dev.hyperstatus

import java.io.BufferedReader
import java.io.InputStreamReader

object RootDiagnostics {
    fun collect(): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c",
                "echo '=== BUILD ==='; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.miui.ui.version.name; " +
                "echo '=== OVERLAYS ==='; cmd overlay list --user 0; " +
                "echo '=== RUNTIME FILES ==='; find /system /system_ext /product /vendor -type f \\( -iname '*rustruntime*' \\) 2>/dev/null | head -40; " +
                "echo '=== PERMISSION XML REFERENCES ==='; grep -Ril 'hyperos.rustruntime.v3' /system/etc/permissions /system_ext/etc/permissions /product/etc/permissions /vendor/etc/permissions 2>/dev/null | head -30"))
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
            p.waitFor()
            (out + if (err.isNotBlank()) "\nERR:\n$err" else "").take(18000)
        } catch (e: Exception) {
            "检测失败：${e.javaClass.simpleName}: ${e.message}\n请确认设备已安装 Magisk/KernelSU，并允许 su 授权。"
        }
    }
}
