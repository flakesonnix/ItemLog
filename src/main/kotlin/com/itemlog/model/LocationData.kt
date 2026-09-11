package com.itemlog.model

import org.bukkit.Location
import org.jetbrains.annotations.Nullable

data class LocationData(
    @Nullable val world: String?,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
) {
    companion object {
        @Nullable
        fun from(loc: Location?): LocationData? {
            if (loc == null) return null
            return LocationData(
                loc.world?.name,
                loc.x,
                loc.y,
                loc.z,
                loc.yaw,
                loc.pitch,
            )
        }
    }
}
