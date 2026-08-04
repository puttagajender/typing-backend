package com.brothers.typing.dto;

public record WeakKeyResponse(
        String character,
        int mistakeCount,
        double mistakePercentage,
        MistakeType dominantMistakeType
) {
}
