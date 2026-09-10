package com.itemlog.model;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class ItemEvent {
    public final UUID eventId;
    public final EventType type;
    public final long timestamp;
    public final @Nullable UUID playerId;
    public final @Nullable LocationData location;
    public final @Nullable ItemSnapshot before;
    public final @Nullable ItemSnapshot after;
    public final @Nullable String source;
    public final @Nullable String material;

    public ItemEvent(UUID eventId, EventType type, long timestamp,
                     @Nullable UUID playerId, @Nullable LocationData location,
                     @Nullable ItemSnapshot before, @Nullable ItemSnapshot after,
                     @Nullable String source) {
        this.eventId = eventId;
        this.type = type;
        this.timestamp = timestamp;
        this.playerId = playerId;
        this.location = location;
        this.before = before;
        this.after = after;
        this.source = source;
        this.material = before != null ? before.material() : after != null ? after.material() : null;
        if (before == null && after == null) {
            throw new IllegalArgumentException("before and after cannot both be null");
        }
    }
}