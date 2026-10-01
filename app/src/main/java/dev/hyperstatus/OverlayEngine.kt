package dev.hyperstatus

import android.content.Context
import com.android.apksig.ApkSigner
import java.io.File
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec

object OverlayEngine {
    private const val SYSTEM_UI = "com.android.systemui"
    private const val FRAMEWORK = "android"
    private const val AAPT2_REMOTE = "/data/local/tmp/hyperstatus_aapt2"
    private const val WORK = "/data/local/tmp/hyperstatus"
    private const val SYS_OVERLAY = "dev.hyperstatus.overlay.systemui"
    private const val FW_OVERLAY = "dev.hyperstatus.overlay.framework"

    // Deliberate allow-list: nothing in the engine can target runtime libraries.
    private val allowedTargets = setOf(SYSTEM_UI, FRAMEWORK)

    fun apply(context: Context, config: TuningConfig): Result {
        if (!RootShell.available()) return Result(false, "未获得 root 授权")
        return try {
            ensureAapt2(context)
            val work = File(context.cacheDir, "overlay-build").apply { deleteRecursively(); mkdirs() }
            val specs = buildSpecs(config)
            if (specs.isEmpty()) return Result(true, "没有需要应用的修改")

            // Compile everything first. No install/enable happens until all builds succeed.
            val built = specs.map { compileOverlay(context, work, it) }
            val failed = built.firstOrNull { !it.ok }
            if (failed != null) return Result(false, failed.message)

            val installErrors = mutableListOf<String>()
            for (item in built) {
                val apk = item.apk ?: continue
                val install = RootShell.run("pm install -r '${apk.absolutePath}'")
                if (!install.ok) installErrors += "${item.packageName}:\n${install.out}\n${install.err}"
            }
            if (installErrors.isNotEmpty()) {
                RootShell.run("cmd overlay disable --user 0 $SYS_OVERLAY >/dev/null 2>&1 || true; cmd overlay disable --user 0 $FW_OVERLAY >/dev/null 2>&1 || true")
                return Result(false, "Overlay 安装失败：\n" + installErrors.joinToString("\n"))
            }

            // Enable only our two known package names. If any enable fails, disable both.
            RootShell.run("cmd overlay disable --user 0 $SYS_OVERLAY >/dev/null 2>&1 || true; cmd overlay disable --user 0 $FW_OVERLAY >/dev/null 2>&1 || true")
            val enableErrors = built.mapNotNull {
                val r = RootShell.run("cmd overlay enable --user 0 ${it.packageName}")
                if (r.ok) null else "${it.packageName}: ${r.out} ${r.err}"
            }
            if (enableErrors.isNotEmpty()) {
                RootShell.run("cmd overlay disable --user 0 $SYS_OVERLAY >/dev/null 2>&1 || true; cmd overlay disable --user 0 $FW_OVERLAY >/dev/null 2>&1 || true")
                return Result(false, "Overlay 启用失败，已自动停用本工具 Overlay：\n" + enableErrors.joinToString("\n"))
            }
            RootShell.run("am force-stop $SYSTEM_UI >/dev/null 2>&1 || true")
            Result(true, "已应用。仅启用 HyperStatus 自己的 Overlay；SystemUI 已重启。顶部 Cutout 首次修改如仍无变化，再重启一次手机。")
        } catch (e: Exception) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            Result(false, "应用失败：${e.javaClass.name}: ${e.message}\n\n${sw}")
        }
    }

    fun restore(): Result {
        if (!RootShell.available()) return Result(false, "未获得 root 授权")
        RootShell.run("cmd overlay disable --user current $SYS_OVERLAY >/dev/null 2>&1 || true; cmd overlay disable --user current $FW_OVERLAY >/dev/null 2>&1 || true")
        val u = RootShell.run("pm uninstall --user 0 $SYS_OVERLAY >/dev/null 2>&1 || true; pm uninstall --user 0 $FW_OVERLAY >/dev/null 2>&1 || true")
        RootShell.run("am force-stop $SYSTEM_UI >/dev/null 2>&1 || true")
        return Result(true, "已恢复默认并停用 HyperStatus Overlay。${u.err.trim()}")
    }

    fun status(): String = RootShell.run("cmd overlay list --user 0 | grep -E 'dev\\.hyperstatus\\.overlay\\.(systemui|framework)' || true").out.trim()

    data class Result(val ok: Boolean, val message: String)

    private data class Spec(
        val packageName: String,
        val target: String,
        val entries: List<Pair<String, String>>
    )

    private data class Built(
        val ok: Boolean,
        val message: String,
        val apk: File?,
        val packageName: String,
    )

    private fun buildSpecs(c: TuningConfig): List<Spec> {
        val out = mutableListOf<Spec>()
        val systemUiEntries = mutableListOf<Pair<String, String>>()
        if (c.startEnabled) systemUiEntries += "dimen/status_bar_padding_start" to "${c.startDp}dp"
        if (c.endEnabled) systemUiEntries += "dimen/status_bar_padding_end" to "${c.endDp}dp"
        if (c.heightEnabled) systemUiEntries += "dimen/status_bar_height" to "${c.heightDp}dp"
        if (systemUiEntries.isNotEmpty()) out += Spec(SYS_OVERLAY, SYSTEM_UI, systemUiEntries)

        if (c.heightEnabled || c.notchKiller) {
            val fw = mutableListOf<Pair<String, String>>()
            if (c.heightEnabled) {
                fw += "dimen/status_bar_height" to "${c.heightDp}dp"
                fw += "dimen/status_bar_height_default" to "${c.heightDp}dp"
                fw += "dimen/status_bar_height_portrait" to "${c.heightDp}dp"
                fw += "dimen/status_bar_height_landscape" to "${c.heightDp}dp"
            }
            if (c.notchKiller) {
                fw += "bool/config_fillMainBuiltInDisplayCutout" to "false"
                fw += "bool/config_maskMainBuiltInDisplayCutout" to "true"
                fw += "string/config_mainBuiltInDisplayCutout" to "M 0,0 L 0, 0 C 0,0 0,0 0,0"
                fw += "string/config_mainBuiltInDisplayCutoutRectApproximation" to "@string/config_mainBuiltInDisplayCutout"
            }
            out += Spec(FW_OVERLAY, FRAMEWORK, fw)
        }
        return out
    }

    fun ensureAapt2(context: Context) {
        val local = File(context.filesDir, "aapt2-arm64-v8a")
        if (!local.exists() || local.length() < 1024 * 1024) {
            context.resources.openRawResource(R.raw.aapt2_arm64_v8a).use { input ->
                FileOutputStream(local).use { output -> input.copyTo(output) }
            }
        }
        if (!local.canRead() || local.length() < 1024 * 1024) {
            throw IllegalStateException("AAPT2 内置文件无效或未正确打包")
        }
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        if (abi != "arm64-v8a") {
            throw IllegalStateException("当前设备 ABI=$abi；本版本内置 AAPT2 仅支持 arm64-v8a")
        }
        val copy = RootShell.run("mkdir -p '$WORK'; cp '${local.absolutePath}' '$AAPT2_REMOTE'; chmod 755 '$AAPT2_REMOTE'; file '$AAPT2_REMOTE' 2>/dev/null || true")
        if (!copy.ok) throw IllegalStateException("无法准备 Root AAPT2：\n${copy.out}")
    }

    private fun compileOverlay(context: Context, work: File, spec: Spec): Built {
        if (spec.target !in allowedTargets || spec.entries.any { it.first.contains("rustruntime", true) }) {
            return Built(false, "目标/资源不在 HyperStatus 安全白名单内", null, spec.packageName)
        }
        val dir = File(work, spec.packageName.replace('.', '_')).apply { mkdirs() }
        val res = File(dir, "res/values").apply { mkdirs() }
        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
            for ((name, value) in spec.entries) {
                val parts = name.split('/', limit = 2)
                val type = parts[0]; val n = parts[1]
                if (type == "string") append("<string name=\"$n\">$value</string>\n")
                else append("<$type name=\"$n\">$value</$type>\n")
            }
            append("</resources>\n")
        }
        File(res, "values.xml").writeText(xml)
        val manifest = File(dir, "AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="${spec.packageName}">
                <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35" />
                <overlay xmlns:android="http://schemas.android.com/apk/res/android" android:targetPackage="${spec.target}" android:isStatic="false" android:priority="10" />
                <application android:hasCode="false" android:label="HyperStatus" android:allowBackup="false" />
            </manifest>
        """.trimIndent())
        val compiled = File(dir, "compiled").apply { mkdirs() }
        val unsigned = File(dir, "unsigned.apk")
        val signed = File(dir, "${spec.packageName}.apk")
        val framework = "/system/framework/framework-res.apk"
        val target = if (spec.target == SYSTEM_UI) RootShell.run("pm path $SYSTEM_UI | sed 's/^package://' | grep '\\.apk$' | tr '\\n' ' '").out.trim() else ""
        if (spec.target == FRAMEWORK && !RootShell.run("test -f $framework").ok) return Built(false, "找不到 framework-res.apk", null, spec.packageName)
        if (spec.target == SYSTEM_UI && target.isBlank()) return Built(false, "找不到 com.android.systemui APK", null, spec.packageName)
        val inputArgs = buildString {
            append("-I '$framework'")
            if (target.isNotBlank()) for (p in target.split(Regex("\\s+"))) append(" -I '$p'")
        }
        val targetApks = if (spec.target == SYSTEM_UI) target.split(Regex("\\s+")).filter { it.isNotBlank() } else listOf(framework)
        val missing = spec.entries.filterNot { entryExists(targetApks, entry = entryName(entry.first)) }
        if (missing.isNotEmpty()) {
            return Built(false, "ROM 未提供这些资源，已阻止修改：\n" + missing.joinToString("\n") { "- ${it.first}" }, null, spec.packageName)
        }
        val compile = RootShell.run("rm -rf '${compiled.absolutePath}'; mkdir -p '${compiled.absolutePath}'; '$AAPT2_REMOTE' compile --dir '${res.parentFile!!.absolutePath}' -o '${compiled.absolutePath}'")
        if (!compile.ok) return Built(false, "AAPT2 compile 失败：\n${compile.out}\n${compile.err}", null, spec.packageName)
        val link = RootShell.run("'$AAPT2_REMOTE' link -o '${unsigned.absolutePath}' $inputArgs --manifest '${manifest.absolutePath}' '${compiled.absolutePath}'/*.flat --auto-add-overlay")
        if (!link.ok) return Built(false, "AAPT2 link 失败：\n${link.out}\n${link.err}", null, spec.packageName)
        sign(context, unsigned, signed)
        return Built(true, "ok", signed, spec.packageName)
    }


    private fun entryName(typeAndName: String): String = typeAndName.substringAfter('/')

    private fun entryExists(targetApks: List<String>, entry: String): Boolean {
        return targetApks.any { apk ->
            val r = RootShell.run("'$AAPT2_REMOTE' dump resources '$apk' 2>/dev/null | grep -F -m 1 '$entry'")
            r.ok && r.out.isNotBlank()
        }
    }

    private fun sign(context: Context, input: File, output: File) {
        val key = loadPrivateKey(context)
        val cert = loadCertificate(context)
        val config = ApkSigner.SignerConfig.Builder("HyperStatus", key, listOf(cert)).build()
        ApkSigner.Builder(listOf(config))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }

    private fun loadPrivateKey(context: Context): PrivateKey {
        val bytes = context.assets.open("Keys/hyperstatus.pk8").use { it.readBytes() }
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun loadCertificate(context: Context): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(context.assets.open("Keys/hyperstatus.x509.der")) as X509Certificate
    }
}
