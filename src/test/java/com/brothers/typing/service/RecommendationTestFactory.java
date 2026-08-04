package com.brothers.typing.service;

import com.brothers.typing.config.RecommendationProperties;
import com.brothers.typing.config.RecommendationProperties.CategoryRule;
import com.brothers.typing.config.RecommendationProperties.DifficultyPromotionRule;
import com.brothers.typing.dto.RecommendationCategory;
import com.brothers.typing.dto.RecommendedDifficulty;
import com.brothers.typing.dto.TypingLevel;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

final class RecommendationTestFactory {

    private RecommendationTestFactory() {
    }

    static RecommendationService recommendationService() {
        return new RecommendationService(new ConfigurableRecommendationRuleEngine(properties()));
    }

    static RecommendationProperties properties() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setMinimumAccuracy(60);
        properties.setMinimumWpm(10);
        properties.setMaximumMistakes(20);
        properties.setRecommendedDuration(60);
        properties.setDefaultLevel(TypingLevel.BEGINNER);
        properties.setDefaultDifficulty(RecommendedDifficulty.EASY);
        properties.setDefaultCategory(RecommendationCategory.GENERAL);
        properties.setDefaultReason("Build consistency before increasing difficulty.");
        properties.setDifficultyPromotionRules(List.of(
                promotion(TypingLevel.INTERMEDIATE, RecommendedDifficulty.MEDIUM,
                        75, 25, 15, 90, "Intermediate practice"),
                promotion(TypingLevel.ADVANCED, RecommendedDifficulty.HARD,
                        90, 45, 8, 120, "Advanced practice"),
                promotion(TypingLevel.EXPERT, RecommendedDifficulty.EXPERT,
                        97, 70, 3, 180, "Expert practice")));
        properties.setCategoryRules(List.of(
                category(70, 20, RecommendationCategory.COMMON_WORDS),
                category(85, 10, RecommendationCategory.JAVA),
                category(95, 5, RecommendationCategory.PROGRAMMING)));
        return properties;
    }

    static WeakKeyAnalysisService weakKeyAnalysisService() {
        com.brothers.typing.config.WeakKeyProperties properties =
                new com.brothers.typing.config.WeakKeyProperties();
        properties.setMinimumSpaceMistakes(3);
        properties.setMaximumWeakKeys(5);
        properties.setNoWeakKeysSummary("No weak keys detected.");
        Map<String, List<String>> words = new LinkedHashMap<>();
        words.put("r", List.of("correct", "remember", "practice"));
        words.put("t", List.of("typing", "better", "today"));
        words.put("e", List.of("every", "speed", "exercise"));
        properties.setPracticeWords(words);
        return new WeakKeyAnalysisService(properties);
    }

    private static DifficultyPromotionRule promotion(
            TypingLevel level,
            RecommendedDifficulty difficulty,
            double accuracy,
            double wpm,
            int mistakes,
            int duration,
            String reason) {
        DifficultyPromotionRule rule = new DifficultyPromotionRule();
        rule.setLevel(level);
        rule.setDifficulty(difficulty);
        rule.setMinimumAccuracy(accuracy);
        rule.setMinimumWpm(wpm);
        rule.setMaximumMistakes(mistakes);
        rule.setRecommendedDuration(duration);
        rule.setReason(reason);
        return rule;
    }

    private static CategoryRule category(
            double accuracy, int mistakes, RecommendationCategory category) {
        CategoryRule rule = new CategoryRule();
        rule.setMinimumAccuracy(accuracy);
        rule.setMaximumMistakes(mistakes);
        rule.setCategory(category);
        return rule;
    }
}
