package com.itemlog.model;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class Restoration {
    public final UUID restorationId;
    public final UUID eventId;
    public final UUID adminId;
    public final UUID targetId;
    public final long timestamp;
    @Nullable public final LocationData restoreLocation;
    @Nullable public final String resultJson;
    public final String status;

    public Restoration(UUID restorationId, UUID eventId, UUID adminId, UUID targetId,
                       long timestamp, @Nullable LocationData restoreLocation,
                       @Nullable String resultJson, String status) {
        this.restorationId = restorationId;
        this.eventId = eventId;
        this.adminId = adminId;
        this.targetId = targetId;
        this.timestamp = timestamp;
        this.restoreLocation = restoreLocation;
        this.resultJson = resultJson;
        this.status = status;
    }
}