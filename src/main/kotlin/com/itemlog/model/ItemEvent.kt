package com.itemlog.model

import org.jetbrains.annotations.Nullable
import java.util.UUID

data class ItemEvent(
    val eventId: UUID,
    val type: EventType,
    val timestamp: Long,
    @Nullable val playerId: UUID?,
    @Nullable val location: LocationData?,
    @Nullable val before: ItemSnapshot?,
    @Nullable val after: ItemSnapshot?,
    @Nullable val source: String?
) {
    val material: String?
        get() = before?.material ?: after?.material

    init {
        if (before == null && after == null) {
            throw IllegalArgumentException("before and after cannot both be null")
        }
    }
}