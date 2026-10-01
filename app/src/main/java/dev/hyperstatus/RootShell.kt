package dev.hyperstatus

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object RootShell {
    fun available(): Boolean = run("id", 5_000).ok

    fun run(vararg commands: String): ShellResult {
        return run(commands.joinToString("\n"), 30_000)
    }

    fun run(command: String, timeoutMs: Long): ShellResult {
        return try {
            val p = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = Thread {
                BufferedReader(InputStreamReader(p.inputStream)).useLines { lines ->
                    lines.forEach { output.append(it).append('\n') }
                }
            }
            reader.start()

            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly()
                reader.join(500)
                return ShellResult(124, output.toString(), "timeout after ${timeoutMs}ms")
            }
            reader.join(2_000)
            val code = p.exitValue()
            ShellResult(code, output.toString(), "")
        } catch (e: Exception) {
            ShellResult(-1, "", "${e.javaClass.simpleName}: ${e.message ?: ""}")
        }
    }
}
