package dev.hyperstatus.overlay

object IconifyResources {
    const val SYSTEM_UI = "com.android.systemui"
    const val FRAMEWORK = "android"

    fun buildSystemUi(startDp: Int?, endDp: Int?, heightDp: Int?): OverlaySpec? {
        val resources = buildList {
            startDp?.let { add(ResourceSpec(SYSTEM_UI, "dimen", "status_bar_padding_start", "${it}dp")) }
            endDp?.let { add(ResourceSpec(SYSTEM_UI, "dimen", "status_bar_padding_end", "${it}dp")) }
            heightDp?.let { add(ResourceSpec(SYSTEM_UI, "dimen", "status_bar_height", "${it}dp")) }
        }
        return resources.takeIf { it.isNotEmpty() }?.let {
            OverlaySpec(
                packageName = "dev.hyperstatus.overlay.systemui",
                category = "hyperstatus_iconify_lite_systemui",
                targetPackage = SYSTEM_UI,
                resources = it
            )
        }
    }

    fun buildFramework(heightDp: Int?, notchKiller: Boolean): OverlaySpec? {
        val resources = buildList {
            heightDp?.let {
                add(ResourceSpec(FRAMEWORK, "dimen", "status_bar_height", "${it}dp"))
                add(ResourceSpec(FRAMEWORK, "dimen", "status_bar_height_default", "${it}dp"))
                add(ResourceSpec(FRAMEWORK, "dimen", "status_bar_height_portrait", "${it}dp"))
                add(ResourceSpec(FRAMEWORK, "dimen", "status_bar_height_landscape", "${it}dp"))
            }
            if (notchKiller) {
                val emptyPath = "M 0,0 L 0,0 C 0,0 0,0 0,0"
                add(ResourceSpec(FRAMEWORK, "bool", "config_fillMainBuiltInDisplayCutout", "false"))
                add(ResourceSpec(FRAMEWORK, "bool", "config_maskMainBuiltInDisplayCutout", "true"))
                add(ResourceSpec(FRAMEWORK, "string", "config_mainBuiltInDisplayCutout", emptyPath))
                // Exactly mirrors Iconify: reference the path resource above.
                add(ResourceSpec(FRAMEWORK, "string", "config_mainBuiltInDisplayCutoutRectApproximation", "@string/config_mainBuiltInDisplayCutout"))
            }
        }
        return resources.takeIf { it.isNotEmpty() }?.let {
            OverlaySpec(
                packageName = "dev.hyperstatus.overlay.framework",
                category = "hyperstatus_iconify_lite_framework",
                targetPackage = FRAMEWORK,
                resources = it
            )
        }
    }
}
