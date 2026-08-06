package com.brothers.typing.learning.recovery.service;

import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.recovery.dto.KeyPerformanceRequest;
import com.brothers.typing.learning.recovery.dto.RecoveryExerciseType;
import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryRequest;
import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryResponse;
import com.brothers.typing.learning.service.LessonWordDictionary;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakKeyRecoveryServiceTest {

    private static final List<String> HOME_KEYS = List.of("a", "s", "d", "f", "j", "k", "l", ";");
    private final WeakKeyRecoveryProperties properties = new WeakKeyRecoveryProperties();
    private final WeakKeyAnalysisService analysis = new WeakKeyAnalysisService(properties);
    private final HomeRowFingerMap fingerMap = new HomeRowFingerMap();
    private final LessonWordDictionary dictionary = new LessonWordDictionary();
    private final WeakKeyRecoveryService service = new WeakKeyRecoveryService(
            analysis, properties, fingerMap, dictionary, () -> 42L);

    @Test
    void noMeaningfulWeakKeyReturnsPositiveNoRecoveryResponse() {
        WeakKeyRecoveryResponse response = service.generate(request(
                HOME_KEYS, List.of(performance("s", 20, 1, 0)), 3, List.of()));

        assertFalse(response.recoveryRequired());
        assertTrue(response.weakKeys().isEmpty());
        assertTrue(response.exercises().isEmpty());
        assertEquals("No focused recovery is needed. Continue normal practice.", response.reason());
    }

    @Test
    void singleWeakKeyUsesOrderedRecoveryProgression() {
        WeakKeyRecoveryResponse response = service.generate(request(
                HOME_KEYS, List.of(performance("s", 25, 6, 2)), 3, List.of()));

        assertTrue(response.recoveryRequired());
        assertEquals(List.of(
                RecoveryExerciseType.SINGLE_KEY,
                RecoveryExerciseType.HOME_MOVEMENT_PAIR,
                RecoveryExerciseType.NEIGHBOUR_PAIR,
                RecoveryExerciseType.SHORT_PATTERN,
                RecoveryExerciseType.WORD,
                RecoveryExerciseType.MIXED_REVIEW),
                response.exercises().stream().limit(6).map(exercise -> exercise.type()).toList());
        assertTrue(response.exercises().get(1).content().contains("as")
                || response.exercises().get(1).content().contains("ds"));
    }

    @Test
    void multipleKeysAreLimitedToThreeAndHighestKeyGetsMoreExercises() {
        WeakKeyRecoveryResponse response = service.generate(request(HOME_KEYS, List.of(
                performance("a", 20, 4, 0), performance("s", 20, 8, 0),
                performance("d", 20, 5, 0), performance("f", 20, 3, 0)), 5, List.of()));

        assertEquals(List.of("s", "d", "a"),
                response.weakKeys().stream().map(key -> key.key()).toList());
        long highestCount = response.exercises().stream()
                .filter(exercise -> exercise.targetKeys().contains("s")).count();
        long thirdCount = response.exercises().stream()
                .filter(exercise -> exercise.targetKeys().contains("a")).count();
        assertTrue(highestCount > thirdCount);
    }

    @Test
    void dictionaryWordsContainWeakKeyAndOnlyLearnedKeys() {
        WeakKeyRecoveryResponse response = service.generate(request(HOME_KEYS,
                List.of(performance("s", 20, 6, 0)), 3, List.of()));

        assertTrue(response.exercises().stream()
                .filter(exercise -> exercise.type() == RecoveryExerciseType.WORD)
                .allMatch(exercise -> exercise.content().contains("s")));
        assertAllContentSafe(response, Set.copyOf(HOME_KEYS));
    }

    @Test
    void noValidWordsSkipsWordPhaseWithoutIntroducingKeys() {
        WeakKeyRecoveryResponse response = service.generate(request(
                List.of(";"), List.of(performance(";", 20, 5, 0)), 3, List.of()));

        assertTrue(response.exercises().stream()
                .noneMatch(exercise -> exercise.type() == RecoveryExerciseType.WORD));
        assertAllContentSafe(response, Set.of(";"));
    }

    @Test
    void generatedExercisesAreUniqueAndApproximatelyMatchDuration() {
        WeakKeyRecoveryResponse response = service.generate(request(
                HOME_KEYS, List.of(performance("s", 20, 5, 0)), 3, List.of()));
        int duration = response.exercises().stream()
                .mapToInt(exercise -> exercise.estimatedDurationSeconds()).sum();

        assertEquals(response.exercises().size(), new HashSet<>(response.exercises().stream()
                .map(exercise -> exercise.content()).toList()).size());
        assertEquals(response.exercises().size(), new HashSet<>(response.exercises().stream()
                .map(exercise -> exercise.id()).toList()).size());
        assertTrue(duration >= 150 && duration <= 195);
    }

    @Test
    void completedExerciseIdsAreNotReturnedAndFinalReviewSuppressesRecentRecovery() {
        WeakKeyRecoveryRequest initialRequest = request(
                HOME_KEYS, List.of(performance("s", 20, 5, 0)), 3, List.of());
        WeakKeyRecoveryResponse initial = service.generate(initialRequest);
        List<String> completedIds = initial.exercises().stream().map(exercise -> exercise.id()).toList();

        WeakKeyRecoveryResponse repeated = service.generate(request(
                HOME_KEYS, List.of(performance("s", 20, 5, 0)), 3, completedIds));

        assertFalse(repeated.recoveryRequired());
        assertTrue(repeated.exercises().isEmpty());
    }

    @Test
    void seededGenerationIsDeterministic() {
        WeakKeyRecoveryRequest request = request(
                HOME_KEYS, List.of(performance("s", 20, 5, 0)), 3, List.of());
        WeakKeyRecoveryResponse first = service.generate(request);
        WeakKeyRecoveryResponse same = new WeakKeyRecoveryService(
                analysis, properties, fingerMap, dictionary, () -> 42L).generate(request);
        WeakKeyRecoveryResponse different = new WeakKeyRecoveryService(
                analysis, properties, fingerMap, dictionary, () -> 99L).generate(request);

        assertEquals(first, same);
        assertNotEquals(first.exercises().get(3).content(), different.exercises().get(3).content());
    }

    @Test
    void unsupportedLessonKeyAndDurationAreRejected() {
        assertThrows(WeakKeyRecoveryException.class, () -> service.generate(
                new WeakKeyRecoveryRequest("UNKNOWN", HOME_KEYS,
                        List.of(performance("s", 20, 5, 0)), List.of(), 3)));
        assertThrows(WeakKeyRecoveryException.class, () -> service.generate(request(
                List.of("q"), List.of(performance("q", 20, 5, 0)), 3, List.of())));
        assertThrows(WeakKeyRecoveryException.class, () -> service.generate(request(
                HOME_KEYS, List.of(performance("s", 20, 5, 0)), 6, List.of())));
    }

    private WeakKeyRecoveryRequest request(
            List<String> learnedKeys,
            List<KeyPerformanceRequest> performance,
            int duration,
            List<String> completedIds) {
        return new WeakKeyRecoveryRequest(
                "HOME_ROW_1", learnedKeys, performance, completedIds, duration);
    }

    private KeyPerformanceRequest performance(String key, int attempts, int mistakes, int consecutive) {
        return new KeyPerformanceRequest(key, attempts, mistakes, consecutive);
    }

    private void assertAllContentSafe(WeakKeyRecoveryResponse response, Set<String> learnedKeys) {
        assertTrue(response.exercises().stream().allMatch(exercise ->
                exercise.content().codePoints().allMatch(codePoint -> codePoint == ' '
                        || learnedKeys.contains(new String(Character.toChars(codePoint))))));
    }
}
