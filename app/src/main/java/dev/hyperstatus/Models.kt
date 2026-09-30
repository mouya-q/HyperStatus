package dev.hyperstatus

data class ScanResult(
    val root: Boolean,
    val sdk: Int,
    val release: String,
    val miuiVersion: String,
    val frameworkRes: String?,
    val systemUiApks: List<String>,
    val systemUiResources: Set<String>,
    val frameworkResources: Set<String>,
    val overlayableSystemUi: String,
    val overlayableFramework: String,
    val rustruntimeRefs: List<String>,
    val ourOverlays: String,
    val raw: String,
)

data class TuningConfig(
    val notchKiller: Boolean,
    val heightEnabled: Boolean,
    val heightDp: Int,
    val startEnabled: Boolean,
    val startDp: Int,
    val endEnabled: Boolean,
    val endDp: Int,
)
