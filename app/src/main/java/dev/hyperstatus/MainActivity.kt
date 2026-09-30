package dev.hyperstatus

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var scan: TextView
    private lateinit var notch: Switch
    private lateinit var heightSwitch: Switch
    private lateinit var startSwitch: Switch
    private lateinit var endSwitch: Switch
    private lateinit var heightValue: TextView
    private lateinit var startValue: TextView
    private lateinit var endValue: TextView
    private lateinit var heightSeek: SeekBar
    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(246, 247, 249)
        buildUi()
        showRootState()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(32))
            setBackgroundColor(Color.rgb(246, 247, 249))
        }
        page.addView(text("HyperStatus", 30, Color.rgb(25, 29, 38), Typeface.BOLD))
        page.addView(text("STATUS BAR TUNING", 11, Color.GRAY, Typeface.BOLD).also { it.setPadding(0, dp(2), 0, dp(14)) })
        status = text("正在检测 root…", 13, Color.DKGRAY, Typeface.NORMAL)
        page.addView(status)
        page.addView(space(14))

        val note = card("安全边界", "本工具只允许修改 android 与 com.android.systemui 的状态栏资源。不会安装 LSPosed、不修改 /system_ext/framework、不会触碰 hyperos.rustruntime.v3。")
        page.addView(note)

        page.addView(sectionTitle("顶部空间"))
        val notchCard = cardContainer()
        notch = Switch(this).apply {
            text = "Notch Bar Killer"
            textSize = 17f
            setTextColor(Color.rgb(25, 29, 38))
            setOnCheckedChangeListener { _, checked ->
                if (checked) AlertDialog.Builder(this@MainActivity)
                    .setTitle("启用顶部 Cutout 修改？")
                    .setMessage("这是唯一会覆盖 framework-res Cutout 资源的功能。它与 V3 Runtime 无关，但仍属于高风险资源修改；出现异常时可以在本应用里一键恢复默认。")
                    .setNegativeButton("取消") { _, _ -> notch.isChecked = false }
                    .setPositiveButton("继续", null)
                    .show()
            }
        }
        notchCard.addView(notch)
        notchCard.addView(text("按 Iconify 的实现，修改 framework-res 的 Cutout 配置来消除顶部预留区域。", 12, Color.GRAY, Typeface.NORMAL))
        page.addView(notchCard)

        page.addView(sectionTitle("尺寸与间距"))
        page.addView(sliderCard("状态栏高度", 12, 80, 24, "dp") { v -> heightValue.text = "${v}dp" }.also { heightCard ->
            val views = heightCard.tag as Array<View>
            heightSwitch = views[0] as Switch; heightSeek = views[1] as SeekBar; heightValue = views[2] as TextView
        })
        page.addView(sliderCard("左侧间距", 0, 80, 0, "dp") { v -> startValue.text = "${v}dp" }.also { c -> val v=c.tag as Array<View>; startSwitch=v[0] as Switch; startSeek=v[1] as SeekBar; startValue=v[2] as TextView })
        page.addView(sliderCard("右侧间距", 0, 80, 0, "dp") { v -> endValue.text = "${v}dp" }.also { c -> val v=c.tag as Array<View>; endSwitch=v[0] as Switch; endSeek=v[1] as SeekBar; endValue=v[2] as TextView })

        val apply = Button(this).apply {
            text = "应用修改"
            textSize = 16f
            setOnClickListener { applyConfig() }
        }
        page.addView(space(14)); page.addView(apply)
        val restore = Button(this).apply {
            text = "恢复系统默认"
            setOnClickListener { restore() }
        }
        page.addView(restore)

        page.addView(sectionTitle("诊断"))
        val scanButton = Button(this).apply {
            text = "运行只读检测"
            setOnClickListener { runScan() }
        }
        page.addView(scanButton)
        scan = text("尚未运行检测。", 11, Color.DKGRAY, Typeface.NORMAL).apply { setTextIsSelectable(true) }
        page.addView(scan)

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun sliderCard(title: String, min: Int, max: Int, default: Int, unit: String, listener: (Int) -> Unit): LinearLayout {
        val c = cardContainer()
        val sw = Switch(this).apply { text = title; textSize = 17f; isChecked = false }
        val value = text("$default$unit", 14, Color.DKGRAY, Typeface.BOLD)
        val seek = SeekBar(this).apply {
            max = max - min
            progress = default - min
            isEnabled = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { listener(p + min) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        sw.setOnCheckedChangeListener { _, checked -> seek.isEnabled = checked }
        c.addView(LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; addView(sw, LinearLayout.LayoutParams(0, -2, 1f)); addView(value) })
        c.addView(seek)
        c.tag = arrayOf(sw, seek, value)
        return c
    }

    private fun card(title: String, body: String): LinearLayout = cardContainer().apply {
        addView(text(title, 17, Color.rgb(25, 29, 38), Typeface.BOLD))
        addView(text(body, 12, Color.GRAY, Typeface.NORMAL))
    }

    private fun cardContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(18).toFloat() }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(10); layoutParams = lp
    }

    private fun sectionTitle(s: String) = text(s, 14, Color.DKGRAY, Typeface.BOLD).apply { setPadding(dp(4), dp(13), 0, dp(8)) }
    private fun showRootState() { Thread { val ok = RootShell.available(); runOnUiThread { status.text = if (ok) "Root：已授权 · 修改引擎待命" else "Root：未授权 · 点击检测时会触发授权" } }.start() }
    private fun runScan() { scan.text = "检测中…"; Thread { val r=RootDiagnostics.collect(this); runOnUiThread { scan.text=r.raw } }.start() }

    private fun applyConfig() {
        if (!RootShell.available()) { Toast.makeText(this, "没有 root 权限", Toast.LENGTH_LONG).show(); return }
        val cfg = TuningConfig(notch.isChecked, heightSwitch.isChecked, heightSeek.progress+12, startSwitch.isChecked, startSeek.progress, endSwitch.isChecked, endSeek.progress)
        status.text = "正在构建并验证 Overlay…"
        Thread {
            val r = OverlayEngine.apply(this, cfg)
            runOnUiThread { status.text = r.message; Toast.makeText(this, r.message, Toast.LENGTH_LONG).show() }
        }.start()
    }

    private fun restore() {
        AlertDialog.Builder(this).setTitle("恢复系统默认")
            .setMessage("将停用并卸载 HyperStatus 自己安装的两个 Overlay，不触碰其它 Overlay。")
            .setNegativeButton("取消", null).setPositiveButton("恢复") { _, _ -> Thread { val r=OverlayEngine.restore(); runOnUiThread { status.text=r.message; Toast.makeText(this,r.message,Toast.LENGTH_LONG).show() } }.start() }.show()
    }

    private fun text(t:String, size:Int, color:Int, style:Int)=TextView(this).apply{ text=t; textSize=size.toFloat(); setTextColor(color); setTypeface(Typeface.DEFAULT,style) }
    private fun space(h:Int)=Space(this).apply{ layoutParams=LinearLayout.LayoutParams(1,dp(h)) }
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}
