package com.brothers.typing.config;

import com.brothers.typing.dto.RecommendationCategory;
import com.brothers.typing.dto.RecommendedDifficulty;
import com.brothers.typing.dto.TypingLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "typing.recommendation")
public class RecommendationProperties {

    private double minimumAccuracy = 0;
    private double minimumWpm = 0;
    private int maximumMistakes = Integer.MAX_VALUE;
    private int recommendedDuration = 60;
    private TypingLevel defaultLevel = TypingLevel.BEGINNER;
    private RecommendedDifficulty defaultDifficulty = RecommendedDifficulty.EASY;
    private RecommendationCategory defaultCategory = RecommendationCategory.GENERAL;
    private String defaultReason = "Build consistency before increasing difficulty.";
    private List<DifficultyPromotionRule> difficultyPromotionRules = new ArrayList<>();
    private List<CategoryRule> categoryRules = new ArrayList<>();

    public double getMinimumAccuracy() {
        return minimumAccuracy;
    }

    public void setMinimumAccuracy(double minimumAccuracy) {
        this.minimumAccuracy = minimumAccuracy;
    }

    public double getMinimumWpm() {
        return minimumWpm;
    }

    public void setMinimumWpm(double minimumWpm) {
        this.minimumWpm = minimumWpm;
    }

    public int getMaximumMistakes() {
        return maximumMistakes;
    }

    public void setMaximumMistakes(int maximumMistakes) {
        this.maximumMistakes = maximumMistakes;
    }

    public int getRecommendedDuration() {
        return recommendedDuration;
    }

    public void setRecommendedDuration(int recommendedDuration) {
        this.recommendedDuration = recommendedDuration;
    }

    public TypingLevel getDefaultLevel() {
        return defaultLevel;
    }

    public void setDefaultLevel(TypingLevel defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    public RecommendedDifficulty getDefaultDifficulty() {
        return defaultDifficulty;
    }

    public void setDefaultDifficulty(RecommendedDifficulty defaultDifficulty) {
        this.defaultDifficulty = defaultDifficulty;
    }

    public RecommendationCategory getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(RecommendationCategory defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public String getDefaultReason() {
        return defaultReason;
    }

    public void setDefaultReason(String defaultReason) {
        this.defaultReason = defaultReason;
    }

    public List<DifficultyPromotionRule> getDifficultyPromotionRules() {
        return difficultyPromotionRules;
    }

    public void setDifficultyPromotionRules(List<DifficultyPromotionRule> difficultyPromotionRules) {
        this.difficultyPromotionRules = difficultyPromotionRules;
    }

    public List<CategoryRule> getCategoryRules() {
        return categoryRules;
    }

    public void setCategoryRules(List<CategoryRule> categoryRules) {
        this.categoryRules = categoryRules;
    }

    public static class DifficultyPromotionRule {
        private TypingLevel level;
        private RecommendedDifficulty difficulty;
        private double minimumAccuracy;
        private double minimumWpm;
        private int maximumMistakes;
        private int recommendedDuration;
        private String reason;

        public TypingLevel getLevel() {
            return level;
        }

        public void setLevel(TypingLevel level) {
            this.level = level;
        }

        public RecommendedDifficulty getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(RecommendedDifficulty difficulty) {
            this.difficulty = difficulty;
        }

        public double getMinimumAccuracy() {
            return minimumAccuracy;
        }

        public void setMinimumAccuracy(double minimumAccuracy) {
            this.minimumAccuracy = minimumAccuracy;
        }

        public double getMinimumWpm() {
            return minimumWpm;
        }

        public void setMinimumWpm(double minimumWpm) {
            this.minimumWpm = minimumWpm;
        }

        public int getMaximumMistakes() {
            return maximumMistakes;
        }

        public void setMaximumMistakes(int maximumMistakes) {
            this.maximumMistakes = maximumMistakes;
        }

        public int getRecommendedDuration() {
            return recommendedDuration;
        }

        public void setRecommendedDuration(int recommendedDuration) {
            this.recommendedDuration = recommendedDuration;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class CategoryRule {
        private double minimumAccuracy;
        private int maximumMistakes;
        private RecommendationCategory category;

        public double getMinimumAccuracy() {
            return minimumAccuracy;
        }

        public void setMinimumAccuracy(double minimumAccuracy) {
            this.minimumAccuracy = minimumAccuracy;
        }

        public int getMaximumMistakes() {
            return maximumMistakes;
        }

        public void setMaximumMistakes(int maximumMistakes) {
            this.maximumMistakes = maximumMistakes;
        }

        public RecommendationCategory getCategory() {
            return category;
        }

        public void setCategory(RecommendationCategory category) {
            this.category = category;
        }
    }
}
