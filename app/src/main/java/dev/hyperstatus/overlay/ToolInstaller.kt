package dev.hyperstatus.overlay

import android.content.Context
import android.os.Build
import dev.hyperstatus.RootShell
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ToolInstaller {
    private const val ASSET = "Tools/jniLibs.zip"

    fun ensure(context: Context): Pair<File, File> {
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it in setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }
            ?: throw IllegalStateException("不支持的 ABI：${Build.SUPPORTED_ABIS.joinToString()}")
        val dir = File(context.filesDir, "hyperstatus/bin/$abi")
        dir.mkdirs()
        val aapt2 = File(dir, "aapt2")
        val zipalign = File(dir, "zipalign")
        if (aapt2.isFile && zipalign.isFile && aapt2.length() > 1024 * 1024 && zipalign.length() > 1024) {
            RootShell.run("chmod 755 '${aapt2.absolutePath}' '${zipalign.absolutePath}'")
            return aapt2 to zipalign
        }

        val zip = File(context.cacheDir, "hyperstatus-jniLibs.zip")
        context.assets.open(ASSET).use { input ->
            FileOutputStream(zip).use { output -> input.copyTo(output) }
        }
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith("$abi/")) {
                    val leaf = entry.name.substringAfterLast('/')
                    if (leaf == "libaapt2.so" || leaf == "libzipalign.so") {
                        val out = when (leaf) {
                            "libaapt2.so" -> aapt2
                            else -> zipalign
                        }
                        BufferedOutputStream(FileOutputStream(out)).use { bos -> zis.copyTo(bos) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        zip.delete()
        if (!aapt2.isFile || !zipalign.isFile) {
            throw IllegalStateException("内置 jniLibs.zip 中没有找到 $abi 的 aapt2/zipalign")
        }
        RootShell.run("chmod 755 '${aapt2.absolutePath}' '${zipalign.absolutePath}'")
        return aapt2 to zipalign
    }
}
