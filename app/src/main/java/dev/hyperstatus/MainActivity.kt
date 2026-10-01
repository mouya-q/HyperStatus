package dev.hyperstatus

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ComposeView(this).apply {
            setContent { HyperStatusApp() }
        })
    }

    @Composable
    private fun HyperStatusApp() {
        MiuixTheme(
            controller = androidx.compose.runtime.remember {
                ThemeController(ColorSchemeMode.System)
            }
        ) {
            HyperStatusScreen()
        }
    }

    @Composable
    private fun HyperStatusScreen() {
        var notchKiller by rememberSaveable { mutableStateOf(false) }
        var heightEnabled by rememberSaveable { mutableStateOf(false) }
        var startEnabled by rememberSaveable { mutableStateOf(false) }
        var endEnabled by rememberSaveable { mutableStateOf(false) }
        var height by rememberSaveable { mutableStateOf(24) }
        var start by rememberSaveable { mutableStateOf(0) }
        var end by rememberSaveable { mutableStateOf(0) }
        var status by rememberSaveable { mutableStateOf("正在检查 Root…") }
        var log by rememberSaveable { mutableStateOf("HyperStatus 0.3.0\n尚未运行诊断。") }
        val clipboard = LocalClipboardManager.current

        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = "HyperStatus",
                    subtitle = "状态栏调校 · Root 安全模式"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.background)
                    .padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding()
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = status,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "本工具不使用 LSPosed/Xposed，不修改 hyperos.rustruntime.*。"
                    )
                }

                Text(
                    text = "顶部空间",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "Notch Bar Killer",
                        summary = "按 Iconify 对应资源思路处理 framework Cutout 预留区域。建议先单独测试。",
                        checked = notchKiller,
                        onCheckedChange = { notchKiller = it }
                    )
                }

                Text(
                    text = "尺寸与间距",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "启用状态栏高度",
                        summary = "关闭时保留系统默认值",
                        checked = heightEnabled,
                        onCheckedChange = { heightEnabled = it }
                    )
                    if (heightEnabled) {
                        SliderPreference(
                            value = height.toFloat(),
                            onValueChange = { height = it.toInt().coerceIn(12, 160) },
                            title = "高度",
                            summary = "12–160 dp",
                            valueText = "${height}dp",
                            valueRange = 12f..160f,
                            steps = 147
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "启用左侧间距",
                        summary = "状态栏左侧 Padding",
                        checked = startEnabled,
                        onCheckedChange = { startEnabled = it }
                    )
                    if (startEnabled) {
                        SliderPreference(
                            value = start.toFloat(),
                            onValueChange = { start = it.toInt().coerceIn(0, 120) },
                            title = "左侧间距",
                            summary = "0–120 dp",
                            valueText = "${start}dp",
                            valueRange = 0f..120f,
                            steps = 119
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "启用右侧间距",
                        summary = "状态栏右侧 Padding",
                        checked = endEnabled,
                        onCheckedChange = { endEnabled = it }
                    )
                    if (endEnabled) {
                        SliderPreference(
                            value = end.toFloat(),
                            onValueChange = { end = it.toInt().coerceIn(0, 120) },
                            title = "右侧间距",
                            summary = "0–120 dp",
                            valueText = "${end}dp",
                            valueRange = 0f..120f,
                            steps = 119
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val cfg = TuningConfig(
                                notchKiller,
                                heightEnabled,
                                height,
                                startEnabled,
                                start,
                                endEnabled,
                                end
                            )
                            status = "正在构建并验证 Overlay…"
                            Thread {
                                val result = OverlayEngine.apply(this@MainActivity, cfg)
                                runOnUiThread {
                                    status = result.message.lineSequence().firstOrNull() ?: "应用完成"
                                    log = "$log\n\n[APPLY]\n${result.message}"
                                    Toast.makeText(this@MainActivity, status, Toast.LENGTH_LONG).show()
                                }
                            }.start()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("应用修改") }

                    Button(
                        onClick = {
                            Thread {
                                val result = OverlayEngine.restore()
                                runOnUiThread {
                                    status = result.message.lineSequence().firstOrNull() ?: "已恢复"
                                    log = "$log\n\n[RESTORE]\n${result.message}"
                                    Toast.makeText(this@MainActivity, status, Toast.LENGTH_LONG).show()
                                }
                            }.start()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("恢复默认") }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "只读诊断",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("输出区域支持长按选择、系统复制；错误信息不会只放在 Toast 里。")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                log = "$log\n\n[SCAN]\n检测中…"
                                Thread {
                                    try {
                                        val result = RootDiagnostics.collect(this@MainActivity)
                                        runOnUiThread {
                                            status = if (result.root) "Root：可用 · 检测完成" else "Root：不可用"
                                            log = "$log\n${result.raw}"
                                        }
                                    } catch (t: Throwable) {
                                        runOnUiThread {
                                            val msg = "检测异常：${t.javaClass.name}: ${t.message}"
                                            status = "检测失败"
                                            log = "$log\n$msg"
                                        }
                                    }
                                }.start()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("运行检测") }
                        TextButton(
                            text = "复制全部",
                            onClick = {
                                clipboard.setText(AnnotatedString(log))
                                Toast.makeText(this@MainActivity, "日志已复制", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SelectionContainer {
                            Text(
                                text = log,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    TextButton(
                        text = "清空日志",
                        onClick = { log = "HyperStatus 0.3.0\n日志已清空。" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "安全说明",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "修改目标严格限制为 android 与 com.android.systemui。AAPT2 只负责在 root 临时目录构建 Overlay；" +
                            "本项目不会删除、替换或注册 hyperos.rustruntime.v3。构建失败或启用失败时会自动停用本工具自己的 Overlay。"
                    )
                }
                Spacer(Modifier.size(6.dp))
            }
        }
    }
}
