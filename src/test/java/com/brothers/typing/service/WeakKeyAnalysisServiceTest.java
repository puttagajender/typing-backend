package com.brothers.typing.service;

import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeType;
import com.brothers.typing.dto.WeakKeyResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakKeyAnalysisServiceTest {

    private final WeakKeyAnalysisService service =
            RecommendationTestFactory.weakKeyAnalysisService();

    @Test
    void exactMatchReturnsSafeEmptyDefaults() {
        WeakKeyAnalysis result = service.analyze(List.of(match('a'), match('b')));

        assertTrue(result.weakKeys().isEmpty());
        assertEquals("No weak keys detected.", result.weakKeySummary());
        assertTrue(result.suggestedPracticeWords().isEmpty());
    }

    @Test
    void oneWeakKeyIsReturnedWithPercentageAndDominantType() {
        WeakKeyAnalysis result = service.analyze(List.of(wrong('r', 'x')));

        assertEquals(List.of(new WeakKeyResponse(
                "R", 1, 100.0, MistakeType.WRONG_CHARACTER)), result.weakKeys());
        assertEquals("You frequently struggle with R.", result.weakKeySummary());
    }

    @Test
    void multipleWeakKeysAreSortedByHighestCount() {
        WeakKeyAnalysis result = service.analyze(List.of(
                wrong('r', 'x'), missing('t'), wrong('r', 'q'), extra('e')));

        assertEquals(List.of("R", "E", "T"), result.weakKeys().stream()
                .map(WeakKeyResponse::character)
                .toList());
        assertEquals(50.0, result.weakKeys().get(0).mistakePercentage());
    }

    @Test
    void extraCharacterMistakeUsesTypedCharacterAsWeakKey() {
        WeakKeyAnalysis result = service.analyze(List.of(extra('x')));

        assertEquals("X", result.weakKeys().get(0).character());
        assertEquals(MistakeType.EXTRA_CHARACTER,
                result.weakKeys().get(0).dominantMistakeType());
    }

    @Test
    void missingCharacterMistakeUsesExpectedCharacterAsWeakKey() {
        WeakKeyAnalysis result = service.analyze(List.of(missing('t')));

        assertEquals("T", result.weakKeys().get(0).character());
        assertEquals(MistakeType.MISSING_CHARACTER,
                result.weakKeys().get(0).dominantMistakeType());
    }

    @Test
    void wrongCharacterMistakeUsesExpectedCharacterAsWeakKey() {
        WeakKeyAnalysis result = service.analyze(List.of(wrong('e', 'i')));

        assertEquals("E", result.weakKeys().get(0).character());
        assertEquals(MistakeType.WRONG_CHARACTER,
                result.weakKeys().get(0).dominantMistakeType());
    }

    @Test
    void spacesBecomeWeakOnlyAtConfiguredThreshold() {
        WeakKeyAnalysis belowThreshold = service.analyze(List.of(extra(' '), missing(' ')));
        WeakKeyAnalysis atThreshold = service.analyze(
                List.of(extra(' '), missing(' '), wrong(' ', '_')));

        assertTrue(belowThreshold.weakKeys().isEmpty());
        assertEquals("SPACE", atThreshold.weakKeys().get(0).character());
        assertEquals(3, atThreshold.weakKeys().get(0).mistakeCount());
    }

    @Test
    void moreThanFiveWeakKeysAreLimitedToFive() {
        WeakKeyAnalysis result = service.analyze(List.of(
                missing('a'), missing('b'), missing('c'), missing('d'),
                missing('e'), missing('f'), missing('g')));

        assertEquals(5, result.weakKeys().size());
        assertEquals(List.of("A", "B", "C", "D", "E"), result.weakKeys().stream()
                .map(WeakKeyResponse::character)
                .toList());
    }

    @Test
    void equalMistakeCountsUseCharacterOrderForStableResults() {
        WeakKeyAnalysis result = service.analyze(List.of(
                wrong('t', 'x'), wrong('r', 'x'), wrong('e', 'x')));

        assertEquals(List.of("E", "R", "T"), result.weakKeys().stream()
                .map(WeakKeyResponse::character)
                .toList());
    }

    @Test
    void matchItemsAreNeverCountedAsWeakKeys() {
        WeakKeyAnalysis result = service.analyze(List.of(
                match('r'), match('r'), match('t'), match('e')));

        assertTrue(result.weakKeys().isEmpty());
    }

    @Test
    void suggestedWordsComeFromConfiguredLocalWordMap() {
        WeakKeyAnalysis result = service.analyze(List.of(missing('r'), missing('t')));

        assertEquals(List.of("correct", "remember", "practice"),
                result.suggestedPracticeWords().get("R"));
        assertEquals(List.of("typing", "better", "today"),
                result.suggestedPracticeWords().get("T"));
        assertFalse(result.suggestedPracticeWords().containsKey("E"));
    }

    @Test
    void dominantMistakeTypeUsesTheMostFrequentTypeForAKey() {
        WeakKeyAnalysis result = service.analyze(List.of(
                wrong('r', 'x'), missing('r'), missing('r')));

        assertEquals(MistakeType.MISSING_CHARACTER,
                result.weakKeys().get(0).dominantMistakeType());
    }

    private ComparisonDetailResponse match(char character) {
        return new ComparisonDetailResponse(0, 0, character, character, MistakeType.MATCH);
    }

    private ComparisonDetailResponse wrong(char expected, char typed) {
        return new ComparisonDetailResponse(
                0, 0, expected, typed, MistakeType.WRONG_CHARACTER);
    }

    private ComparisonDetailResponse missing(char expected) {
        return new ComparisonDetailResponse(
                0, 0, expected, null, MistakeType.MISSING_CHARACTER);
    }

    private ComparisonDetailResponse extra(char typed) {
        return new ComparisonDetailResponse(
                0, 0, null, typed, MistakeType.EXTRA_CHARACTER);
    }
}
