package com.itemlog.model

import org.jetbrains.annotations.Nullable

data class ItemSnapshot(
    val material: String,
    val amount: Int,
    @Nullable val itemJson: String? = null,
)
