package com.brothers.typing.learning.service;

import com.brothers.typing.learning.dto.ExerciseGenerationRequest;
import com.brothers.typing.learning.dto.ExerciseGenerationResponse;
import com.brothers.typing.learning.dto.ExerciseType;
import com.brothers.typing.learning.dto.LearningDifficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExerciseGenerationServiceTest {

    private static final List<String> HOME_KEYS = List.of("a", "s", "d", "f", "j", "k", "l", ";");
    private final LessonWordDictionary dictionary = new LessonWordDictionary();
    private final SessionPlanningService planner = new SessionPlanningService();
    private final ExerciseGenerationService service =
            new ExerciseGenerationService(dictionary, planner, () -> 42L);

    @ParameterizedTest
    @EnumSource(ExerciseType.class)
    void generatesEverySupportedExerciseTypeWithoutUntaughtKeys(ExerciseType requestedType) {
        ExerciseGenerationResponse response = service.generate(request(requestedType, List.of("s"), 5));

        assertFalse(response.exercises().isEmpty());
        assertEquals(response.exercises().size(), response.estimatedExerciseCount());
        assertTrue(response.exercises().stream().allMatch(exercise -> isSafe(exercise.content())));
        if (requestedType != ExerciseType.MIXED) {
            assertTrue(response.exercises().stream().allMatch(exercise ->
                    exercise.type() == requestedType));
        }
    }

    @Test
    void mixedSessionIsBalancedAndPrioritizesWeakKeys() {
        ExerciseGenerationResponse response = service.generate(request(ExerciseType.MIXED, List.of("s"), 15));

        Set<ExerciseType> types = response.exercises().stream()
                .map(exercise -> exercise.type()).collect(java.util.stream.Collectors.toSet());
        assertTrue(types.containsAll(Set.of(
                ExerciseType.SINGLE_KEY, ExerciseType.MOVEMENT, ExerciseType.WORD,
                ExerciseType.PHRASE, ExerciseType.RANDOM_PATTERN,
                ExerciseType.WEAK_KEY_RECOVERY)));
        assertTrue(response.exercises().stream()
                .filter(exercise -> exercise.type() == ExerciseType.WEAK_KEY_RECOVERY)
                .allMatch(exercise -> exercise.targetKeys().contains("s")));
        assertEquals(20, response.estimatedExerciseCount());
    }

    @Test
    void emptyWeakKeysStillProducesACompleteMixedSession() {
        ExerciseGenerationResponse response = service.generate(request(ExerciseType.MIXED, List.of(), 15));

        assertEquals(20, response.exercises().size());
        assertTrue(response.exercises().stream()
                .noneMatch(exercise -> exercise.type() == ExerciseType.WEAK_KEY_RECOVERY));
    }

    @Test
    void duplicateContentAndIdsArePrevented() {
        ExerciseGenerationResponse response = service.generate(request(ExerciseType.MIXED, List.of("s"), 30));

        assertEquals(response.exercises().size(), new HashSet<>(response.exercises().stream()
                .map(exercise -> exercise.content()).toList()).size());
        assertEquals(response.exercises().size(), new HashSet<>(response.exercises().stream()
                .map(exercise -> exercise.id()).toList()).size());
    }

    @Test
    void previousExerciseIdsAreExcluded() {
        ExerciseGenerationResponse first = service.generate(request(ExerciseType.SINGLE_KEY, List.of("s"), 5));
        String previousId = first.exercises().get(0).id();
        ExerciseGenerationRequest nextRequest = new ExerciseGenerationRequest(
                "HOME_ROW_1", HOME_KEYS, List.of("s"), ExerciseType.SINGLE_KEY,
                LearningDifficulty.BEGINNER, 5, List.of(previousId));

        ExerciseGenerationResponse second = service.generate(nextRequest);

        assertTrue(second.exercises().stream().noneMatch(exercise -> exercise.id().equals(previousId)));
    }

    @Test
    void weakKeyOutsideLearnedKeysIsRejected() {
        ExerciseGenerationRequest invalid = new ExerciseGenerationRequest(
                "HOME_ROW_1", List.of("a", "s"), List.of("d"), ExerciseType.MIXED,
                LearningDifficulty.BEGINNER, 15, List.of());

        ExerciseGenerationException exception = assertThrows(
                ExerciseGenerationException.class, () -> service.generate(invalid));

        assertEquals("Every weak key must exist in learnedKeys", exception.getMessage());
    }

    @Test
    void unsupportedLessonAndLessonKeyAreRejected() {
        assertThrows(ExerciseGenerationException.class, () -> service.generate(
                new ExerciseGenerationRequest("UNKNOWN", HOME_KEYS, List.of(), ExerciseType.MIXED,
                        LearningDifficulty.BEGINNER, 15, List.of())));
        assertThrows(ExerciseGenerationException.class, () -> service.generate(
                new ExerciseGenerationRequest("HOME_ROW_1", List.of("q"), List.of(), ExerciseType.MIXED,
                        LearningDifficulty.BEGINNER, 15, List.of())));
    }

    @Test
    void seededGenerationIsDeterministicAndDifferentSeedsCanVaryPatterns() {
        ExerciseGenerationRequest request = request(ExerciseType.RANDOM_PATTERN, List.of(), 5);
        ExerciseGenerationResponse first = service.generate(request);
        ExerciseGenerationResponse same = new ExerciseGenerationService(dictionary, planner, () -> 42L)
                .generate(request);
        ExerciseGenerationResponse different = new ExerciseGenerationService(dictionary, planner, () -> 99L)
                .generate(request);

        assertEquals(first, same);
        assertNotEquals(first.exercises().get(0).content(), different.exercises().get(0).content());
    }

    @Test
    void minimumAndMaximumDurationsProduceBoundedSessionPlans() {
        ExerciseGenerationResponse minimum = service.generate(request(ExerciseType.MIXED, List.of(), 5));
        ExerciseGenerationResponse maximum = service.generate(request(ExerciseType.MIXED, List.of(), 30));

        assertEquals(7, minimum.estimatedExerciseCount());
        assertEquals(40, maximum.estimatedExerciseCount());
        assertTrue(maximum.exercises().size() <= 40);
    }

    @Test
    void invalidDurationIsRejectedByTheServiceBoundary() {
        ExerciseGenerationRequest invalid = request(ExerciseType.MIXED, List.of(), 4);

        ExerciseGenerationException exception = assertThrows(
                ExerciseGenerationException.class, () -> service.generate(invalid));

        assertEquals("sessionDurationMinutes must be between 5 and 30", exception.getMessage());
    }

    @Test
    void wordAndPhraseExercisesUseOnlyDictionaryWordsAllowedByLearnedKeys() {
        ExerciseGenerationRequest wordRequest = new ExerciseGenerationRequest(
                "HOME_ROW_1", List.of("a", "s"), List.of(), ExerciseType.WORD,
                LearningDifficulty.BEGINNER, 5, List.of());
        ExerciseGenerationResponse words = service.generate(wordRequest);
        ExerciseGenerationResponse phrases = service.generate(new ExerciseGenerationRequest(
                "HOME_ROW_1", List.of("a", "s"), List.of(), ExerciseType.PHRASE,
                LearningDifficulty.BEGINNER, 5, List.of()));

        assertTrue(words.exercises().stream().allMatch(exercise -> exercise.content().matches("[as ]+")));
        assertTrue(phrases.exercises().stream().allMatch(exercise -> exercise.content().matches("[as ]+")));
    }

    @Test
    void noValidWordsFallsBackToSafePatterns() {
        ExerciseGenerationRequest request = new ExerciseGenerationRequest(
                "HOME_ROW_1", List.of(";"), List.of(), ExerciseType.WORD,
                LearningDifficulty.BEGINNER, 5, List.of());

        ExerciseGenerationResponse response = service.generate(request);

        assertTrue(response.exercises().stream()
                .allMatch(exercise -> exercise.type() == ExerciseType.RANDOM_PATTERN));
        assertTrue(response.exercises().stream().allMatch(exercise -> exercise.content().matches("[; ]+")));
    }

    private ExerciseGenerationRequest request(
            ExerciseType type, List<String> weakKeys, int duration) {
        return new ExerciseGenerationRequest(
                "HOME_ROW_1", HOME_KEYS, weakKeys, type,
                LearningDifficulty.BEGINNER, duration, List.of());
    }

    private boolean isSafe(String content) {
        return content.codePoints().allMatch(codePoint ->
                codePoint == ' ' || HOME_KEYS.contains(new String(Character.toChars(codePoint))));
    }
}
