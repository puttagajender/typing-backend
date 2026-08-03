package com.brothers.typing.service;

public record RecommendationInput(
        double correctWpm,
        double grossWpm,
        double accuracy,
        int mistakeCount
) {
}
