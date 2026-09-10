package com.itemlog.model

import org.jetbrains.annotations.Nullable
import java.util.UUID

data class Restoration(
    val restorationId: UUID,
    val eventId: UUID,
    val adminId: UUID,
    val targetId: UUID,
    val timestamp: Long,
    @Nullable val restoreLocation: LocationData?,
    @Nullable val resultJson: String?,
    val status: String
)