package com.itemlog.model;

import org.jetbrains.annotations.Nullable;

public record ItemSnapshot(
    String material,
    int amount,
    @Nullable String itemJson
) {}