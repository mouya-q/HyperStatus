package dev.hyperstatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import androidx.compose.ui.Alignment
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import dev.hyperstatus.overlay.IconifyLiteCompiler

private const val VERSION = BuildConfig.VERSION_NAME

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(androidx.compose.ui.platform.ComposeView(this).apply {
            setContent { App() }
        })
    }

    @Composable
    private fun App() {
        MiuixTheme(
            controller = androidx.compose.runtime.remember { ThemeController(ColorSchemeMode.System) }
        ) {
            MainScreen()
        }
    }

    @Composable
    private fun MainScreen() {
        val prefs = remember { getSharedPreferences("tuning", MODE_PRIVATE) }
        var notch by rememberSaveable { mutableStateOf(prefs.getBoolean("notch", false)) }
        var heightEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("heightEnabled", false)) }
        var startEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("startEnabled", false)) }
        var endEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("endEnabled", false)) }
        var height by rememberSaveable { mutableStateOf(prefs.getInt("height", 24)) }
        var start by rememberSaveable { mutableStateOf(prefs.getInt("start", 0)) }
        var end by rememberSaveable { mutableStateOf(prefs.getInt("end", 0)) }
        var log by rememberSaveable { mutableStateOf("HyperStatus $VERSION\n尚未运行诊断。") }
        var applying by rememberSaveable { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val engine = androidx.compose.runtime.remember { IconifyLiteCompiler(this@MainActivity) }

        fun save() = prefs.edit()
            .putBoolean("notch", notch)
            .putBoolean("heightEnabled", heightEnabled)
            .putBoolean("startEnabled", startEnabled)
            .putBoolean("endEnabled", endEnabled)
            .putInt("height", height)
            .putInt("start", start)
            .putInt("end", end)
            .apply()

        fun runAsync(action: () -> String) {
            if (applying) return
            applying = true
            scope.launch(Dispatchers.IO) {
                val result = try { action() } catch (t: Throwable) {
                    "操作失败：${t.javaClass.name}: ${t.message}"
                }
                launch(Dispatchers.Main) {
                    log = result
                    applying = false
                }
            }
        }

        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = "HyperStatus",
                    subtitle = "Iconify Lite · Root · Miuix"
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(dev.hyperstatus.R.drawable.ic_hyperstatus_mark),
                                contentDescription = "HyperStatus",
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("状态栏调校", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("Iconify Lite · 四项功能", fontSize = 12.sp)
                            }
                            Text("v$VERSION", fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("只保留你需要的四项 Iconify 功能；不使用 LSPosed，不修改 rustruntime。")
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(modifier = Modifier.weight(1f), enabled = !applying) {
                                runAsync { engine.apply(TuningConfig(notch, heightEnabled, height, startEnabled, start, endEnabled, end)).let { it.log } }
                                save()
                            }
                            Button(modifier = Modifier.weight(1f), enabled = !applying) {
                                runAsync { engine.restore().log }
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("应用采用 Iconify 的动态 RRO APK 编译链：AAPT2 → zipalign → 签名。", fontSize = 12.sp)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "去除顶部 Cutout 留白",
                        summary = "Iconify 的 Notch Bar Killer：覆盖 framework 的 4 个 Cutout 资源",
                        checked = notch,
                        onCheckedChange = { notch = it; save() }
                    )
                }

                Card(Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "状态栏高度",
                        summary = "Iconify：SystemUI + framework 的 5 个高度资源",
                        checked = heightEnabled,
                        onCheckedChange = { heightEnabled = it; save() }
                    )
                    if (heightEnabled) {
                        SliderPreference(
                            value = height.toFloat(),
                            onValueChange = { height = it.toInt().coerceIn(0, 240); save() },
                            title = "高度",
                            summary = "0–240 dp；0 也保留为原始 Iconify 输入能力",
                            valueText = "${height} dp",
                            valueRange = 0f..240f,
                            steps = 239
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "左侧状态栏间距",
                        summary = "SystemUI · status_bar_padding_start · 0–120 dp",
                        checked = startEnabled,
                        onCheckedChange = { startEnabled = it; save() }
                    )
                    if (startEnabled) {
                        SliderPreference(
                            value = start.toFloat(),
                            onValueChange = { start = it.toInt().coerceIn(0, 120); save() },
                            title = "左侧",
                            summary = "0–120 dp",
                            valueText = "${start} dp",
                            valueRange = 0f..120f,
                            steps = 119
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "右侧状态栏间距",
                        summary = "SystemUI · status_bar_padding_end · 0–120 dp",
                        checked = endEnabled,
                        onCheckedChange = { endEnabled = it; save() }
                    )
                    if (endEnabled) {
                        SliderPreference(
                            value = end.toFloat(),
                            onValueChange = { end = it.toInt().coerceIn(0, 120); save() },
                            title = "右侧",
                            summary = "0–120 dp",
                            valueText = "${end} dp",
                            valueRange = 0f..120f,
                            steps = 119
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("诊断与日志", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Row {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(log)) }) { Text("复制全部") }
                            Spacer(Modifier.width(2.dp))
                            TextButton(onClick = { log = "" }) { Text("清空") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(modifier = Modifier.weight(1f), enabled = !applying) {
                            runAsync { engine.scan() }
                        }
                        Button(modifier = Modifier.weight(1f), enabled = !applying) {
                            runAsync { engine.status().ifBlank { "没有找到 HyperStatus Lite Overlay。" } }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = log,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth().padding(2.dp)
                        )
                    }
                }

                Text(
                    "安全提示：本项目的保护范围只针对 HyperStatus 自己的 Overlay；恢复时不会删除 Iconify 或其它模块。",
                    fontSize = 12.sp
                )
            }
        }
    }
}
