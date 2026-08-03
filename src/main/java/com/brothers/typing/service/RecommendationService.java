package com.brothers.typing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationRuleEngine ruleEngine;

    public RecommendationService(RecommendationRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public Recommendation recommend(RecommendationInput input) {
        Recommendation recommendation = Objects.requireNonNull(
                ruleEngine.evaluate(input), "Rule engine must return a recommendation");
        Objects.requireNonNull(recommendation.typingLevel(), "Typing level must not be null");
        Objects.requireNonNull(
                recommendation.recommendedDifficulty(), "Difficulty must not be null");
        Objects.requireNonNull(recommendation.recommendedCategory(), "Category must not be null");
        Objects.requireNonNull(recommendation.recommendationReason(), "Reason must not be null");

        log.info("Calculated level: {}", recommendation.typingLevel());
        log.info("Recommended difficulty: {}", recommendation.recommendedDifficulty());
        log.info("Recommended category: {}", recommendation.recommendedCategory());
        log.info("Recommended duration: seconds={}", recommendation.recommendedDuration());
        return recommendation;
    }
}
