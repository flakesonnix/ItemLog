package com.itemlog.model

import org.jetbrains.annotations.Nullable
import org.bukkit.Location

data class LocationData(
    @Nullable val world: String?,
    val x: Double, val y: Double, val z: Double,
    val yaw: Float, val pitch: Float
) {
    companion object {
        @Nullable
        fun from(loc: Location?): LocationData? {
            if (loc == null) return null
            return LocationData(
                loc.world?.name,
                loc.x, loc.y, loc.z,
                loc.yaw, loc.pitch
            )
        }
    }
}