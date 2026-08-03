package com.brothers.typing.service;

import com.brothers.typing.config.RecommendationProperties;
import com.brothers.typing.config.RecommendationProperties.CategoryRule;
import com.brothers.typing.config.RecommendationProperties.DifficultyPromotionRule;
import com.brothers.typing.dto.RecommendationCategory;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ConfigurableRecommendationRuleEngine implements RecommendationRuleEngine {

    private final RecommendationProperties properties;

    public ConfigurableRecommendationRuleEngine(RecommendationProperties properties) {
        this.properties = properties;
    }

    @Override
    public Recommendation evaluate(RecommendationInput input) {
        Recommendation recommendation = defaultRecommendation();
        if (!meetsBaseline(input)) {
            return recommendation;
        }

        RecommendationCategory category = recommendationCategory(input);
        return properties.getDifficultyPromotionRules().stream()
                .filter(rule -> qualifies(input, rule))
                .max(Comparator.comparingInt(rule -> rule.getLevel().ordinal()))
                .map(rule -> promotedRecommendation(rule, category))
                .orElseGet(() -> withCategory(recommendation, category));
    }

    private boolean meetsBaseline(RecommendationInput input) {
        return input.accuracy() >= properties.getMinimumAccuracy()
                && input.correctWpm() >= properties.getMinimumWpm()
                && input.grossWpm() >= properties.getMinimumWpm()
                && input.mistakeCount() <= properties.getMaximumMistakes();
    }

    private boolean qualifies(RecommendationInput input, DifficultyPromotionRule rule) {
        return rule.getLevel() != null
                && rule.getDifficulty() != null
                && input.accuracy() >= rule.getMinimumAccuracy()
                && input.correctWpm() >= rule.getMinimumWpm()
                && input.grossWpm() >= rule.getMinimumWpm()
                && input.mistakeCount() <= rule.getMaximumMistakes();
    }

    private RecommendationCategory recommendationCategory(RecommendationInput input) {
        return properties.getCategoryRules().stream()
                .filter(rule -> rule.getCategory() != null)
                .filter(rule -> input.accuracy() >= rule.getMinimumAccuracy())
                .filter(rule -> input.mistakeCount() <= rule.getMaximumMistakes())
                .max(Comparator.comparingDouble(CategoryRule::getMinimumAccuracy))
                .map(CategoryRule::getCategory)
                .orElse(properties.getDefaultCategory());
    }

    private Recommendation defaultRecommendation() {
        return new Recommendation(
                properties.getDefaultLevel(),
                properties.getDefaultDifficulty(),
                properties.getDefaultCategory(),
                properties.getRecommendedDuration(),
                properties.getDefaultReason());
    }

    private Recommendation promotedRecommendation(
            DifficultyPromotionRule rule, RecommendationCategory category) {
        return new Recommendation(
                rule.getLevel(),
                rule.getDifficulty(),
                category,
                rule.getRecommendedDuration() > 0
                        ? rule.getRecommendedDuration()
                        : properties.getRecommendedDuration(),
                rule.getReason() == null || rule.getReason().isBlank()
                        ? properties.getDefaultReason()
                        : rule.getReason());
    }

    private Recommendation withCategory(
            Recommendation recommendation, RecommendationCategory category) {
        return new Recommendation(
                recommendation.typingLevel(),
                recommendation.recommendedDifficulty(),
                category,
                recommendation.recommendedDuration(),
                recommendation.recommendationReason());
    }
}
