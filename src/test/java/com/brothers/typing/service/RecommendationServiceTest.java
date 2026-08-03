package com.brothers.typing.service;

import com.brothers.typing.dto.RecommendationCategory;
import com.brothers.typing.dto.RecommendedDifficulty;
import com.brothers.typing.dto.TypingLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecommendationServiceTest {

    private final RecommendationService service = RecommendationTestFactory.recommendationService();

    @Test
    void veryLowWpmReturnsBeginnerDefaults() {
        Recommendation recommendation = service.recommend(input(5, 6, 100, 0));

        assertRecommendation(recommendation, TypingLevel.BEGINNER,
                RecommendedDifficulty.EASY, RecommendationCategory.GENERAL, 60);
    }

    @Test
    void mediumWpmPromotesToIntermediate() {
        Recommendation recommendation = service.recommend(input(30, 32, 80, 5));

        assertRecommendation(recommendation, TypingLevel.INTERMEDIATE,
                RecommendedDifficulty.MEDIUM, RecommendationCategory.COMMON_WORDS, 90);
    }

    @Test
    void highWpmAndExcellentAccuracyPromoteToAdvanced() {
        Recommendation recommendation = service.recommend(input(55, 58, 94, 4));

        assertRecommendation(recommendation, TypingLevel.ADVANCED,
                RecommendedDifficulty.HARD, RecommendationCategory.JAVA, 120);
    }

    @Test
    void expertThresholdsPromoteToExpert() {
        Recommendation recommendation = service.recommend(input(70, 72, 97, 3));

        assertRecommendation(recommendation, TypingLevel.EXPERT,
                RecommendedDifficulty.EXPERT, RecommendationCategory.PROGRAMMING, 180);
    }

    @Test
    void poorAccuracyPreventsPromotionDespiteHighWpm() {
        Recommendation recommendation = service.recommend(input(80, 90, 59.99, 2));

        assertRecommendation(recommendation, TypingLevel.BEGINNER,
                RecommendedDifficulty.EASY, RecommendationCategory.GENERAL, 60);
    }

    @Test
    void tooManyMistakesPreventPromotion() {
        Recommendation recommendation = service.recommend(input(80, 90, 99, 21));

        assertRecommendation(recommendation, TypingLevel.BEGINNER,
                RecommendedDifficulty.EASY, RecommendationCategory.GENERAL, 60);
    }

    @Test
    void exactIntermediateBoundaryPromotes() {
        Recommendation recommendation = service.recommend(input(25, 30, 75, 15));

        assertEquals(TypingLevel.INTERMEDIATE, recommendation.typingLevel());
        assertEquals(RecommendedDifficulty.MEDIUM, recommendation.recommendedDifficulty());
    }

    @Test
    void valueBelowIntermediateBoundaryDoesNotPromote() {
        Recommendation recommendation = service.recommend(input(24.99, 30, 75, 15));

        assertEquals(TypingLevel.BEGINNER, recommendation.typingLevel());
    }

    @Test
    void oneHundredPercentAccuracyUsesHighestQualifyingCategory() {
        Recommendation recommendation = service.recommend(input(50, 50, 100, 0));

        assertEquals(RecommendationCategory.PROGRAMMING,
                recommendation.recommendedCategory());
    }

    @Test
    void zeroAccuracyReturnsSafeNonNullDefaults() {
        Recommendation recommendation = service.recommend(input(0, 20, 0, 10));

        assertRecommendation(recommendation, TypingLevel.BEGINNER,
                RecommendedDifficulty.EASY, RecommendationCategory.GENERAL, 60);
        assertNotNull(recommendation.recommendationReason());
    }

    @Test
    void categoryRuleUsesMistakeLimitAsWellAsAccuracy() {
        Recommendation recommendation = service.recommend(input(30, 35, 96, 11));

        assertEquals(RecommendationCategory.COMMON_WORDS,
                recommendation.recommendedCategory());
    }

    @Test
    void allLevelDisplayNamesAreStable() {
        assertEquals("Turtle", TypingLevel.BEGINNER.getDisplayName());
        assertEquals("Rabbit", TypingLevel.INTERMEDIATE.getDisplayName());
        assertEquals("Horse", TypingLevel.ADVANCED.getDisplayName());
        assertEquals("Cheetah", TypingLevel.EXPERT.getDisplayName());
    }

    private RecommendationInput input(
            double correctWpm, double grossWpm, double accuracy, int mistakes) {
        return new RecommendationInput(correctWpm, grossWpm, accuracy, mistakes);
    }

    private void assertRecommendation(
            Recommendation recommendation,
            TypingLevel level,
            RecommendedDifficulty difficulty,
            RecommendationCategory category,
            int duration) {
        assertEquals(level, recommendation.typingLevel());
        assertEquals(difficulty, recommendation.recommendedDifficulty());
        assertEquals(category, recommendation.recommendedCategory());
        assertEquals(duration, recommendation.recommendedDuration());
        assertNotNull(recommendation.recommendationReason());
    }
}
