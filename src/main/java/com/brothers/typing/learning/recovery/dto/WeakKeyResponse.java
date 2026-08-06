package com.brothers.typing.learning.recovery.dto;

public record WeakKeyResponse(
        String key,
        int mistakeCount,
        double mistakePercentage,
        WeakKeyPriority priority
) {
}
