package com.brothers.typing.learning.recovery.service;

import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.recovery.dto.KeyPerformanceRequest;
import com.brothers.typing.learning.recovery.dto.WeakKeyPriority;
import com.brothers.typing.learning.recovery.dto.WeakKeyResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service("recoveryWeakKeyAnalysisService")
public class WeakKeyAnalysisService {

    private final WeakKeyRecoveryProperties properties;

    public WeakKeyAnalysisService(WeakKeyRecoveryProperties properties) {
        this.properties = properties;
    }

    public List<WeakKeyResponse> analyze(
            List<KeyPerformanceRequest> performance, Set<String> learnedKeys) {
        return performance.stream()
                .peek(item -> validate(item, learnedKeys))
                .filter(item -> item.attemptCount() >= properties.getMinimumAttempts())
                .map(this::classify)
                .filter(result -> result.priority() != null)
                .sorted(Comparator
                        .comparingInt((AnalysedKey key) -> priorityRank(key.priority()))
                        .thenComparing(AnalysedKey::mistakePercentage, Comparator.reverseOrder())
                        .thenComparing(AnalysedKey::mistakeCount, Comparator.reverseOrder())
                        .thenComparing(AnalysedKey::key))
                .limit(Math.max(0, Math.min(3, properties.getMaximumWeakKeys())))
                .map(result -> new WeakKeyResponse(
                        result.key(), result.mistakeCount(),
                        round(result.mistakePercentage()), result.priority()))
                .toList();
    }

    private void validate(KeyPerformanceRequest item, Set<String> learnedKeys) {
        if (!learnedKeys.contains(item.key())) {
            throw new WeakKeyRecoveryException("Every performance key must exist in learnedKeys");
        }
        if (item.attemptCount() <= 0 || item.mistakeCount() < 0
                || item.consecutiveMistakes() < 0) {
            throw new WeakKeyRecoveryException("Performance counts are invalid");
        }
        if (item.mistakeCount() > item.attemptCount()) {
            throw new WeakKeyRecoveryException("mistakeCount cannot exceed attemptCount");
        }
    }

    private AnalysedKey classify(KeyPerformanceRequest item) {
        double percentage = (double) item.mistakeCount() / item.attemptCount() * 100.0;
        WeakKeyPriority priority;
        if (percentage >= properties.getHighMistakePercentage()
                || item.consecutiveMistakes() >= properties.getConsecutiveMistakeThreshold()) {
            priority = WeakKeyPriority.HIGH;
        } else if (percentage >= properties.getMediumMistakePercentage()
                || item.mistakeCount() >= properties.getMediumMistakeCount()) {
            priority = WeakKeyPriority.MEDIUM;
        } else if (item.mistakeCount() > 0) {
            priority = WeakKeyPriority.LOW;
        } else {
            priority = null;
        }
        return new AnalysedKey(item.key(), item.mistakeCount(), percentage, priority);
    }

    private int priorityRank(WeakKeyPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record AnalysedKey(
            String key, int mistakeCount, double mistakePercentage, WeakKeyPriority priority) {
    }
}
