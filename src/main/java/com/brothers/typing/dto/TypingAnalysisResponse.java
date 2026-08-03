package com.brothers.typing.dto;

import java.util.List;

public record TypingAnalysisResponse(
        double correctWpm,
        double grossWpm,
        double wpm,
        double accuracy,
        long durationInSeconds,
        int mistakeCount,
        int wrongCharacterCount,
        int missingCharacterCount,
        int extraCharacterCount,
        List<MistakeDetailResponse> mistakeDetails,
        List<ComparisonDetailResponse> comparisonDetails,
        TypingLevel typingLevel,
        String typingLevelDisplayName,
        RecommendedDifficulty recommendedDifficulty,
        RecommendationCategory recommendedCategory,
        int recommendedDuration,
        String recommendationReason
) {
}
