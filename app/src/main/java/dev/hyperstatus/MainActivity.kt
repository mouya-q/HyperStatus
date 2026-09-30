package dev.hyperstatus

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var report: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(246,247,249))
        }
        root.addView(TextView(this).apply {
            text = "HyperStatus\nSTATUS BAR TUNING"
            textSize = 27f; setTextColor(Color.rgb(24,29,38))
        })
        root.addView(TextView(this).apply {
            text = "小米 10 · HyperOS 4 移植版\n安全模式：只读检测，不修改系统"
            textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,12,0,18)
        })
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18,18,18,18)
            setBackgroundColor(Color.WHITE)
        }
        card.addView(TextView(this).apply {
            text = "兼容性检测"; textSize = 19f; setTextColor(Color.BLACK)
        })
        card.addView(TextView(this).apply {
            text = "先确认 ROM 资源和 Overlay 策略，再决定是否开放调节。"
            textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,8,0,12)
        })
        val btn = Button(this).apply {
            text = "运行只读检测"
            setOnClickListener { runCheck() }
        }
        card.addView(btn)
        report = TextView(this).apply {
            text = "尚未检测"; textSize = 12f; setTextColor(Color.rgb(45,52,64))
            setPadding(0,14,0,0)
        }
        card.addView(report)
        root.addView(card, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(TextView(this).apply {
            text = "\n待验证功能\n• 顶部 Cutout / Insets 留白\n• 状态栏高度\n• 左侧间距\n• 右侧间距\n\n当前版本不会应用任何修改。"
            textSize = 15f; setTextColor(Color.rgb(45,52,64))
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun runCheck() {
        report.text = "正在请求 root 并读取系统信息…"
        Thread {
            val result = RootDiagnostics.collect()
            runOnUiThread { report.text = result }
        }.start()
    }
}
