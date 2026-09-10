package com.itemlog.model;

import org.jetbrains.annotations.Nullable;

public record LocationData(
    @Nullable String world,
    double x, double y, double z,
    float yaw, float pitch
) {
    public static LocationData from(org.bukkit.Location loc) {
        return new LocationData(
            loc.getWorld() != null ? loc.getWorld().getName() : null,
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch()
        );
    }
}