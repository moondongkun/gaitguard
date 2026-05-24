package com.gaitguard.app.data.model

sealed class TerrainType(
    val label: String,
    val icon: String,
    val symmetryThresholdAdjust: Float = 0f,   // positive = relax (raise acceptable asymmetry)
    val fallThresholdTighten: Boolean = false
) {
    object FLAT             : TerrainType("Flat Ground",       "🚶", 0f,    false)
    object STAIRS_ASC       : TerrainType("Stairs ↑",          "🪜", 0.08f, false)
    object STAIRS_DESC      : TerrainType("Stairs ↓",          "🪜", 0f,    true)
    object INCLINE          : TerrainType("Incline",           "⛰",  0f,    false)
    object UNEVEN           : TerrainType("Uneven Terrain",    "🌱", 0.12f, false)

    companion object {
        val all: List<TerrainType> = listOf(FLAT, STAIRS_ASC, STAIRS_DESC, INCLINE, UNEVEN)
    }
}
