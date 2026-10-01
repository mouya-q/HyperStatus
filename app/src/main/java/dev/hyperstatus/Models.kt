package dev.hyperstatus

data class TuningConfig(
    val notchKiller: Boolean,
    val heightEnabled: Boolean,
    val heightDp: Int,
    val startEnabled: Boolean,
    val startDp: Int,
    val endEnabled: Boolean,
    val endDp: Int,
)

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok: Boolean get() = code == 0
    fun text(): String = buildString {
        if (out.isNotBlank()) append(out.trim())
        if (err.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(err.trim())
        }
    }.ifBlank { "(无命令输出)" }
}
