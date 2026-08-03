package com.brothers.typing.service;

import com.brothers.typing.dto.RecommendationCategory;
import com.brothers.typing.dto.RecommendedDifficulty;
import com.brothers.typing.dto.TypingLevel;

public record Recommendation(
        TypingLevel typingLevel,
        RecommendedDifficulty recommendedDifficulty,
        RecommendationCategory recommendedCategory,
        int recommendedDuration,
        String recommendationReason
) {
}
