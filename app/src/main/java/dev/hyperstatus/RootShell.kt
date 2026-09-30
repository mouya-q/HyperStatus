package dev.hyperstatus

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok: Boolean get() = code == 0
}

object RootShell {
    fun run(command: String, timeoutMs: Long = 30_000): ShellResult {
        return try {
            val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = BufferedReader(InputStreamReader(p.inputStream)).readText()
            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly()
                return ShellResult(124, output, "timeout")
            }
            if (p.exitValue() == 0) ShellResult(0, output, "") else ShellResult(p.exitValue(), output, "")
        } catch (e: Exception) {
            ShellResult(-1, "", e.javaClass.simpleName + ": " + (e.message ?: ""))
        }
    }

    fun available(): Boolean = run("id").ok
}
