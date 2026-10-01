package dev.hyperstatus.overlay

data class ResourceSpec(
    val target: String,
    val type: String,
    val name: String,
    val value: String,
)

data class OverlaySpec(
    val packageName: String,
    val category: String,
    val targetPackage: String,
    val resources: List<ResourceSpec>,
)
