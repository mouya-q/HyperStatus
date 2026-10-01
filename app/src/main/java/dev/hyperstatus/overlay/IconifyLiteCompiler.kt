package dev.hyperstatus.overlay

import android.content.Context
import dev.hyperstatus.RootShell
import dev.hyperstatus.ShellResult
import dev.hyperstatus.apksigner.CryptoUtils
import dev.hyperstatus.apksigner.SignAPK
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class IconifyLiteCompiler(private val context: Context) {
    companion object {
        const val SYSTEM_UI = "com.android.systemui"
        const val FRAMEWORK = "android"
        const val SYSTEM_OVERLAY_DIR = "/system/product/overlay"
        const val MODULE_DIR = "/data/adb/modules/HyperStatusLite"
        const val MODULE_OVERLAY_DIR = "$MODULE_DIR/system/product/overlay"
        const val LEGACY_SYSTEMUI = "dev.hyperstatus.overlay.systemui"
        const val LEGACY_FRAMEWORK = "dev.hyperstatus.overlay.framework"
    }

    data class BuildOutput(val packageName: String, val apk: File, val log: String)

    private val workDir = File(context.filesDir, "hyperstatus/work")
    private val unsignedUnaligned = File(workDir, "unsigned-unaligned")
    private val unsigned = File(workDir, "unsigned")
    private val signed = File(workDir, "signed")

    fun apply(config: dev.hyperstatus.TuningConfig): Result {
        if (!RootShell.available()) return Result(false, "未获得 root 授权。\n请给 HyperStatus root 权限后重试。")
        return try {
            val preflight = preflight(config)
            if (!preflight.ok) return Result(false, preflight.log)

            prepareModuleDir()
            disableOurOverlays()
            cleanupLegacy()
            ensureTools()

            val built = mutableListOf<BuildOutput>()
            val systemUi = IconifyResources.buildSystemUi(
                if (config.startEnabled) config.startDp else null,
                if (config.endEnabled) config.endDp else null,
                if (config.heightEnabled) config.heightDp else null
            )
            val framework = IconifyResources.buildFramework(
                if (config.heightEnabled) config.heightDp else null,
                config.notchKiller
            )
            systemUi?.let { built += buildOverlay(it) }
            framework?.let { built += buildOverlay(it) }

            removeInstalledOverlayIfUnused(SYSTEM_UI, systemUi != null)
            removeInstalledOverlayIfUnused(FRAMEWORK, framework != null)
            for (output in built) {
                deployOverlay(output)
            }

            val live = tryEnableLive(built.map { it.packageName })
            val text = buildString {
                append("Iconify Lite 资源 Overlay 已生成。\n\n")
                append("目标资源与值沿用 Iconify 对应实现。\n")
                append("本次仅处理 android / com.android.systemui。\n")
                append("没有修改、删除或替换 hyperos.rustruntime.*。\n\n")
                append(live)
                append("\n\n构建日志：\n")
                built.forEach { append("==== ").append(it.packageName).append(" ====\n").append(it.log).append('\n') }
            }
            Result(true, text)
        } catch (t: Throwable) {
            Result(false, "应用失败：${stackTrace(t)}")
        }
    }

    fun restore(): Result {
        if (!RootShell.available()) return Result(false, "未获得 root 授权。")
        return try {
            disableOurOverlays()
            cleanupLegacy()
            removeFilesByPrefix(SYSTEM_OVERLAY_DIR, listOf("dev.hyperstatus.iconifylite.systemui", "dev.hyperstatus.iconifylite.framework"))
            removeFilesByPrefix(MODULE_OVERLAY_DIR, listOf("dev.hyperstatus.iconifylite.systemui", "dev.hyperstatus.iconifylite.framework"))
            val restart = RootShell.run("killall $SYSTEM_UI")
            Result(true, buildString {
                append("已恢复 HyperStatus Lite。\n")
                append("只移除了 HyperStatus Lite 自己的 Overlay 文件。\n")
                append("未触碰 framework-res、SystemUI 或 hyperos.rustruntime.*。\n")
                append(if (restart.ok) "SystemUI 已重启。" else "SystemUI 重启失败：${restart.text()}")
            })
        } catch (t: Throwable) {
            Result(false, "恢复失败：${stackTrace(t)}")
        }
    }


    fun status(): String = RootShell.run("cmd overlay list --user 0 | grep -F 'dev.hyperstatus.iconifylite' || true").text()

    fun scan(): String {
        return try {
            val lines = mutableListOf<String>()
            lines += "=== HyperStatus Iconify Lite safety scan ==="
            lines += "Root: ${RootShell.available()}"
            lines += "Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
            lines += "ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}"
            lines += "MIUI/HyperOS property: ${RootShell.run("getprop ro.miui.ui.version.name").out.firstOrNull().orEmpty().ifBlank { "(empty)" }}"
            lines += "framework-res: ${findFirstExisting(listOf("/system/framework/framework-res.apk", "/system_ext/framework/framework-res.apk"))}"
            lines += "SystemUI: ${RootShell.run("pm path $SYSTEM_UI").text()}"
            val targets = listOf(
                SYSTEM_UI to listOf("status_bar_padding_start", "status_bar_padding_end", "status_bar_height"),
                FRAMEWORK to listOf("status_bar_height", "status_bar_height_default", "status_bar_height_portrait", "status_bar_height_landscape", "config_fillMainBuiltInDisplayCutout", "config_maskMainBuiltInDisplayCutout", "config_mainBuiltInDisplayCutout", "config_mainBuiltInDisplayCutoutRectApproximation")
            )
            for ((pkg, names) in targets) {
                lines += "--- $pkg ---"
                for (name in names) {
                    val apk = if (pkg == SYSTEM_UI) RootShell.run("pm path $SYSTEM_UI").out.firstOrNull()?.substringAfter("package:") else findFirstExisting(listOf("/system/framework/framework-res.apk", "/system_ext/framework/framework-res.apk"))
                    if (apk.isNullOrBlank() || apk == "(none)") {
                        lines += "$name: target APK unavailable"
                    } else {
                        val r = dumpResourceSearch(apk, name)
                        lines += "$name: ${if (r) "FOUND" else "not found / dump failed"}"
                    }
                }
            }
            lines += "--- V3 runtime references (read-only) ---"
            lines += RootShell.run("grep -Ril 'hyperos.rustruntime.v3' /product/etc/permissions /system_ext/etc/permissions /system/etc/permissions /vendor/etc/permissions 2>/dev/null | head -30").text()
            lines += "--- our overlays ---"
            lines += RootShell.run("cmd overlay list --user 0 | grep -F 'dev.hyperstatus.iconifylite' || true").text()
            lines.joinToString("\n")
        } catch (t: Throwable) {
            "扫描失败：${stackTrace(t)}"
        }
    }

    private fun preflight(config: dev.hyperstatus.TuningConfig): Preflight {
        val scan = scan()
        val failures = mutableListOf<String>()
        if (!RootShell.run("[ -f /system/framework/framework-res.apk ] || [ -f /system_ext/framework/framework-res.apk ]").ok) {
            failures += "framework-res.apk 未找到。"
        }
        val uiApk = RootShell.run("pm path $SYSTEM_UI").out.firstOrNull()?.substringAfter("package:")
        if (uiApk.isNullOrBlank()) failures += "无法定位 com.android.systemui。"

        // Only reject missing resources that the user explicitly asked to modify.
        val uiChecks = buildList {
            if (config.startEnabled) add("$SYSTEM_UI status_bar_padding_start")
            if (config.endEnabled) add("$SYSTEM_UI status_bar_padding_end")
            if (config.heightEnabled) add("$SYSTEM_UI status_bar_height")
        }
        val fwChecks = buildList {
            if (config.heightEnabled) add("$FRAMEWORK status_bar_height")
            if (config.heightEnabled) add("$FRAMEWORK status_bar_height_default")
            if (config.heightEnabled) add("$FRAMEWORK status_bar_height_portrait")
            if (config.heightEnabled) add("$FRAMEWORK status_bar_height_landscape")
            if (config.notchKiller) addAll(listOf(
                "$FRAMEWORK config_fillMainBuiltInDisplayCutout",
                "$FRAMEWORK config_maskMainBuiltInDisplayCutout",
                "$FRAMEWORK config_mainBuiltInDisplayCutout",
                "$FRAMEWORK config_mainBuiltInDisplayCutoutRectApproximation"
            ))
        }
        for (check in uiChecks + fwChecks) {
            val parts = check.split(' ', limit = 2)
            val pkg = parts[0]
            val name = parts[1]
            val apk = if (pkg == SYSTEM_UI) uiApk else findFirstExisting(listOf("/system/framework/framework-res.apk", "/system_ext/framework/framework-res.apk"))
            if (apk.isNullOrBlank() || !dumpResourceSearch(apk, name)) failures += "资源未找到：$pkg:$name"
        }
        val dangerousReference = RootShell.run("grep -Ril 'hyperos.rustruntime.v3' ${context.filesDir.absolutePath}/. 2>/dev/null | head -1").text()
        if (dangerousReference.isNotBlank()) failures += "内部工作目录出现 rustruntime 关键字，已停止。"
        if (failures.isNotEmpty()) return Preflight(false, scan + "\n\n=== 预检失败 ===\n" + failures.joinToString("\n"))
        return Preflight(true, scan + "\n\n=== 预检通过 ===")
    }

    private data class Preflight(val ok: Boolean, val log: String)

    private fun buildOverlay(spec: OverlaySpec): BuildOutput {
        val safeName = if (spec.targetPackage == SYSTEM_UI) "dev.hyperstatus.iconifylite.systemui" else "dev.hyperstatus.iconifylite.framework"
        val source = File(workDir, safeName.replace('.', '_'))
        source.deleteRecursively()
        File(source, "res/values").mkdirs()
        File(source, "AndroidManifest.xml").writeText(createManifest(safeName, spec.targetPackage, spec.category))
        val values = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
            spec.resources.forEach { r ->
                append("    <${r.type} name=\"${r.name}\">")
                append(escapeXml(r.value))
                append("</${r.type}>\n")
            }
            append("</resources>\n")
        }
        File(source, "res/values/iconify_lite.xml").writeText(values)

        val (aapt2, zipalign) = ToolInstaller.ensure(context)
        val unaligned = File(unsignedUnaligned, "$safeName-unsigned-unaligned.apk")
        val aligned = File(unsigned, "$safeName-unsigned.apk")
        val output = File(signed, "$safeName.apk")
        unsignedUnaligned.mkdirs(); unsigned.mkdirs(); signed.mkdirs()
        unaligned.delete(); aligned.delete(); output.delete()

        val targetApks = if (spec.targetPackage == SYSTEM_UI) {
            RootShell.run("pm path $SYSTEM_UI").out.mapNotNull { it.substringAfter("package:", "").takeIf { p -> p.endsWith(".apk") } }
        } else emptyList()
        val framework = findFirstExisting(listOf("/system/framework/framework-res.apk", "/system_ext/framework/framework-res.apk"))
            ?: throw IllegalStateException("framework-res.apk missing")

        val compileDir = File(source, "compiled").apply { mkdirs() }
        val compile = RootShell.run(
            "rm -rf '${compileDir.absolutePath}' '${unaligned.absolutePath}'",
            "mkdir -p '${compileDir.absolutePath}'",
            "'${aapt2.absolutePath}' compile --dir '${File(source, "res").absolutePath}' -o '${compileDir.absolutePath}'"
        )
        if (!compile.ok) throw IllegalStateException("AAPT2 compile failed:\n${compile.text()}")
        val imports = buildString {
            append(" -I '").append(framework).append("'")
            targetApks.filter { it != framework }.forEach { append(" -I '").append(it).append("'") }
        }
        val link = RootShell.run(
            "'${aapt2.absolutePath}' link -o '${unaligned.absolutePath}' -I '$framework' --manifest '${File(source, "AndroidManifest.xml").absolutePath}' '${compileDir.absolutePath}'/* --auto-add-overlay$imports"
        )
        if (!link.ok) throw IllegalStateException("AAPT2 link failed:\n${link.text()}")
        val align = RootShell.run("'${zipalign.absolutePath}' -f 4 '${unaligned.absolutePath}' '${aligned.absolutePath}'")
        if (!align.ok) throw IllegalStateException("zipalign failed:\n${align.text()}")

        val cert: X509Certificate = context.assets.open("Keystore/testkey.x509.pem").use { CryptoUtils.readCertificate(it) }
        val key: PrivateKey = context.assets.open("Keystore/testkey.pk8").use { CryptoUtils.readPrivateKey(it) }
        SignAPK.sign(cert, key, aligned.absolutePath, output.absolutePath)
        if (!output.isFile || output.length() == 0L) throw IllegalStateException("signed APK was not created")
        return BuildOutput(safeName, output, "AAPT2/zipalign/sign 成功\n资源：${spec.resources.joinToString { "${it.type}/${it.name}=${it.value}" }}")
    }

    private fun deployOverlay(output: BuildOutput) {
        val name = output.packageName + ".apk"
        val src = output.apk.absolutePath
        val module = "$MODULE_OVERLAY_DIR/$name"
        val direct = "$SYSTEM_OVERLAY_DIR/$name"
        val commands = listOf(
            "mkdir -p '$MODULE_OVERLAY_DIR'",
            "cp -f '$src' '$module'",
            "chmod 0644 '$module'",
            "mount -o remount,rw / >/dev/null 2>&1 || true",
            "mkdir -p '$SYSTEM_OVERLAY_DIR' >/dev/null 2>&1 || true",
            "cp -f '$src' '$direct' >/dev/null 2>&1 || true",
            "chmod 0644 '$direct' >/dev/null 2>&1 || true",
            "mount -o remount,ro / >/dev/null 2>&1 || true"
        )
        val result = RootShell.run(*commands.toTypedArray())
        if (!result.ok) throw IllegalStateException("部署失败：\n${result.text()}")
    }

    private fun tryEnableLive(packages: List<String>): String {
        if (packages.isEmpty()) {
            return "当前配置没有启用任何修改，已移除 HyperStatus Lite Overlay。"
        }
        val list = RootShell.run("cmd overlay list --user 0").out
        val live = mutableListOf<String>()
        val missing = mutableListOf<String>()
        for (pkg in packages.distinct()) {
            if (list.contains(pkg)) {
                val enable = RootShell.run("cmd overlay enable --user 0 '$pkg'", "cmd overlay set-priority '$pkg' highest")
                if (enable.ok) live += pkg else missing += "$pkg：${enable.text()}"
            } else missing += pkg
        }
        if (live.isNotEmpty()) RootShell.run("killall $SYSTEM_UI")
        return buildString {
            if (live.isNotEmpty()) append("本次已发现并启用：${live.joinToString()}\n")
            if (missing.isNotEmpty()) append("当前开机周期未发现：${missing.joinToString()}\n这些 APK 已放入系统 Overlay 目录/持久 Overlay 目录，重启后由系统扫描并生效。\n没有使用 killall system_server，也没有触碰 V3 Runtime。")
            if (live.isEmpty() && missing.isEmpty()) append("没有需要启用的 Overlay。")
        }.trim()
    }

    private fun removeInstalledOverlayIfUnused(target: String, keep: Boolean) {
        if (!keep) {
            val pkg = if (target == SYSTEM_UI) "dev.hyperstatus.iconifylite.systemui" else "dev.hyperstatus.iconifylite.framework"
            RootShell.run("cmd overlay disable --user 0 '$pkg'", "rm -f '$SYSTEM_OVERLAY_DIR/$pkg.apk' '$MODULE_OVERLAY_DIR/$pkg.apk'")
        }
    }

    private fun disableOurOverlays() {
        RootShell.run(
            "cmd overlay disable --user 0 'dev.hyperstatus.iconifylite.systemui' || true",
            "cmd overlay disable --user 0 'dev.hyperstatus.iconifylite.framework' || true"
        )
    }

    private fun cleanupLegacy() {
        RootShell.run("pm uninstall $LEGACY_SYSTEMUI || true", "pm uninstall $LEGACY_FRAMEWORK || true")
        RootShell.run("rm -f '$SYSTEM_OVERLAY_DIR/$LEGACY_SYSTEMUI.apk' '$SYSTEM_OVERLAY_DIR/$LEGACY_FRAMEWORK.apk'")
    }

    private fun prepareModuleDir() {
        RootShell.run(
            "mkdir -p '$MODULE_OVERLAY_DIR'",
            "cat > '$MODULE_DIR/module.prop' <<'EOF'\nid=hyperstatus_lite\nname=HyperStatus Lite Overlay Provider\nversion=0.5.0\nversionCode=5\nauthor=HyperStatus\ndescription=Minimal Iconify-compatible status bar resource overlays\nEOF",
            "cat > '$MODULE_DIR/service.sh' <<'EOF'\n#!/system/bin/sh\nsleep 3\ncmd overlay enable --user 0 dev.hyperstatus.iconifylite.systemui >/dev/null 2>&1 || true\ncmd overlay enable --user 0 dev.hyperstatus.iconifylite.framework >/dev/null 2>&1 || true\ncmd overlay set-priority dev.hyperstatus.iconifylite.systemui highest >/dev/null 2>&1 || true\ncmd overlay set-priority dev.hyperstatus.iconifylite.framework highest >/dev/null 2>&1 || true\nEOF",
            "chmod 0755 '$MODULE_DIR/service.sh'",
            "chmod 0644 '$MODULE_DIR/module.prop'"
        )
    }

    private fun ensureTools() {
        ToolInstaller.ensure(context)
    }

    private fun dumpResourceSearch(apk: String, name: String): Boolean {
        val (aapt2, _) = ToolInstaller.ensure(context)
        val scan = RootShell.run("'${aapt2.absolutePath}' dump resources '${shell(apk)}' 2>/dev/null | grep -F '$name'")
        return scan.ok && scan.out.contains(name)
    }

    private fun findFirstExisting(paths: List<String>): String? = paths.firstOrNull { RootShell.run("[ -f '$it' ]").ok }

    private fun removeFilesByPrefix(dir: String, names: List<String>) {
        val cmd = names.joinToString(" ") { "rm -f '$dir/$it.apk'" }
        RootShell.run(cmd)
    }

    private fun createManifest(packageName: String, targetPackage: String, category: String): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val manifest = doc.createElement("manifest")
        manifest.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        manifest.setAttribute("package", packageName)
        doc.appendChild(manifest)
        val uses = doc.createElement("uses-sdk")
        uses.setAttribute("android:minSdkVersion", "24")
        uses.setAttribute("android:targetSdkVersion", android.os.Build.VERSION.SDK_INT.toString())
        manifest.appendChild(uses)
        val overlay = doc.createElement("overlay")
        overlay.setAttribute("android:category", category)
        overlay.setAttribute("android:priority", "1")
        overlay.setAttribute("android:targetPackage", targetPackage)
        overlay.setAttribute("android:isStatic", "false")
        manifest.appendChild(overlay)
        val app = doc.createElement("application")
        app.setAttribute("android:label", "HyperStatus Lite")
        app.setAttribute("android:allowBackup", "false")
        app.setAttribute("android:hasCode", "false")
        manifest.appendChild(app)
        val sw = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            setOutputProperty(OutputKeys.ENCODING, "utf-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
        }
        transformer.transform(DOMSource(doc), StreamResult(sw))
        return sw.toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun shell(value: String): String = value.replace("'", "'\"'\"'")

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(java.io.PrintWriter(sw))
        return sw.toString()
    }

    data class Result(val ok: Boolean, val log: String)
}
