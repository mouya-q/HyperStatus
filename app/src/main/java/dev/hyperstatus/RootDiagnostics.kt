package dev.hyperstatus

object RootDiagnostics {
    private const val AAPT2 = "/data/local/tmp/hyperstatus_aapt2"
    fun collect(context: android.content.Context): ScanResult {
        val root = RootShell.available()
        if (!root) return ScanResult(false, 0, "?", "?", null, emptyList(), emptySet(), emptySet(), "", "", emptyList(), "", "ROOT 不可用")
        OverlayEngine.ensureAapt2(context)
        val release = RootShell.run("getprop ro.build.version.release").out.trim()
        val sdk = RootShell.run("getprop ro.build.version.sdk").out.trim().toIntOrNull() ?: 0
        val miui = RootShell.run("getprop ro.miui.ui.version.name").out.trim()
        val framework = RootShell.run("test -f /system/framework/framework-res.apk && echo /system/framework/framework-res.apk").out.trim().ifBlank { null }
        val ui = RootShell.run("pm path com.android.systemui | sed 's/^package://' | grep '\\.apk$'").out.lines().filter { it.isNotBlank() }
        val fwRes = if (framework != null) resourceSearch(framework, listOf("status_bar_height", "status_bar_height_default", "status_bar_height_portrait", "status_bar_height_landscape", "config_fillMainBuiltInDisplayCutout", "config_maskMainBuiltInDisplayCutout", "config_mainBuiltInDisplayCutout")) else emptySet()
        val uiRes = ui.flatMap { resourceSearch(it, listOf("status_bar_padding_start", "status_bar_padding_end", "status_bar_height")) }.toSet()
        val overlayableFw = if (framework != null) overlayableSearch(framework) else ""
        val overlayableUi = ui.firstOrNull()?.let { overlayableSearch(it) } ?: ""
        val rust = RootShell.run("grep -Ril 'hyperos.rustruntime.v3' /system/etc/permissions /system_ext/etc/permissions /product/etc/permissions /vendor/etc/permissions 2>/dev/null | head -30").out.lines().filter { it.isNotBlank() }
        val ours = OverlayEngine.status()
        val raw = buildString {
            appendLine("=== HyperStatus safety scan ===")
            appendLine("Root: $root")
            appendLine("Android: $release (SDK $sdk)")
            appendLine("MIUI/HyperOS property: $miui")
            appendLine("framework-res: ${framework ?: "NOT FOUND"}")
            appendLine("SystemUI APKs:")
            ui.forEach { appendLine("  $it") }
            appendLine("SystemUI resources: ${uiRes.sorted().joinToString()}")
            appendLine("Framework resources: ${fwRes.sorted().joinToString()}")
            appendLine("Overlayable(SystemUI): $overlayableUi")
            appendLine("Overlayable(framework): $overlayableFw")
            appendLine("V3 runtime references:")
            rust.forEach { appendLine("  $it") }
            appendLine("Our overlays:")
            appendLine(ours.ifBlank { "  none" })
        }
        return ScanResult(root, sdk, release, miui, framework, ui, uiRes, fwRes, overlayableUi, overlayableFw, rust, ours, raw)
    }

    private fun resourceSearch(apk: String, names: List<String>): Set<String> {
        val regex = names.joinToString("|") { Regex.escape(it) }
        return RootShell.run("'$AAPT2' dump resources '$apk' 2>/dev/null | grep -E '($regex)' | sed -E 's/.*($regex).*/\\1/' | sort -u | head -80").out.lines().filter { it.isNotBlank() }.toSet()
    }

    private fun overlayableSearch(apk: String): String {
        return RootShell.run("'$AAPT2' dump resources '$apk' 2>/dev/null | grep -i -E 'overlayable|overlayable:' | head -30").out.trim()
    }
}
