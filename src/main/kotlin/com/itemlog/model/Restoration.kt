package com.itemlog.model

import java.util.UUID
import org.jetbrains.annotations.Nullable

data class Restoration(
    val restorationId: UUID,
    val eventId: UUID,
    val adminId: UUID,
    val targetId: UUID,
    val timestamp: Long,
    @Nullable val restoreLocation: LocationData?,
    @Nullable val resultJson: String?,
    val status: String,
)
