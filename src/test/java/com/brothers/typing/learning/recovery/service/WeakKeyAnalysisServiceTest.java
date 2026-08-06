package com.brothers.typing.learning.recovery.service;

import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.recovery.dto.KeyPerformanceRequest;
import com.brothers.typing.learning.recovery.dto.WeakKeyPriority;
import com.brothers.typing.learning.recovery.dto.WeakKeyResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakKeyAnalysisServiceTest {

    private final WeakKeyRecoveryProperties properties = new WeakKeyRecoveryProperties();
    private final WeakKeyAnalysisService service = new WeakKeyAnalysisService(properties);

    @Test
    void noMistakesProducesNoWeakKeys() {
        List<WeakKeyResponse> result = service.analyze(
                List.of(performance("s", 20, 0, 0)), Set.of("s"));

        assertTrue(result.isEmpty());
    }

    @Test
    void mediumAndHighThresholdsAreApplied() {
        List<WeakKeyResponse> result = service.analyze(List.of(
                performance("s", 20, 3, 0),
                performance("d", 20, 4, 0)), Set.of("s", "d"));

        assertEquals(WeakKeyPriority.HIGH, result.get(0).priority());
        assertEquals("d", result.get(0).key());
        assertEquals(WeakKeyPriority.MEDIUM, result.get(1).priority());
    }

    @Test
    void consecutiveMistakesTriggerHighPriority() {
        List<WeakKeyResponse> result = service.analyze(
                List.of(performance("s", 20, 1, 3)), Set.of("s"));

        assertEquals(WeakKeyPriority.HIGH, result.get(0).priority());
    }

    @Test
    void tooFewAttemptsAreNotEvaluated() {
        List<WeakKeyResponse> result = service.analyze(
                List.of(performance("s", 9, 9, 9)), Set.of("s"));

        assertTrue(result.isEmpty());
    }

    @Test
    void limitsAndOrdersTheTopThreeKeys() {
        List<WeakKeyResponse> result = service.analyze(List.of(
                performance("a", 20, 4, 0), performance("s", 20, 8, 0),
                performance("d", 20, 5, 0), performance("f", 20, 3, 0)),
                Set.of("a", "s", "d", "f"));

        assertEquals(List.of("s", "d", "a"), result.stream().map(WeakKeyResponse::key).toList());
    }

    @Test
    void invalidKeyRelationshipAndCountsAreRejected() {
        assertThrows(WeakKeyRecoveryException.class, () -> service.analyze(
                List.of(performance("d", 20, 2, 0)), Set.of("s")));
        WeakKeyRecoveryException exception = assertThrows(WeakKeyRecoveryException.class,
                () -> service.analyze(
                        List.of(performance("s", 10, 11, 0)), Set.of("s")));
        assertEquals("mistakeCount cannot exceed attemptCount", exception.getMessage());
    }

    private KeyPerformanceRequest performance(
            String key, int attempts, int mistakes, int consecutive) {
        return new KeyPerformanceRequest(key, attempts, mistakes, consecutive);
    }
}
