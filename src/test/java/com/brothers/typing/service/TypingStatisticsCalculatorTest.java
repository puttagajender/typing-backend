package com.brothers.typing.service;

import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypingStatisticsCalculatorTest {

    @Test
    void calculatesEveryStatisticFromOneConsistentAlignment() {
        List<ComparisonDetailResponse> alignment = List.of(
                detail(MistakeType.MATCH),
                detail(MistakeType.MATCH),
                detail(MistakeType.MATCH),
                detail(MistakeType.MATCH),
                detail(MistakeType.MATCH),
                detail(MistakeType.WRONG_CHARACTER),
                detail(MistakeType.MISSING_CHARACTER),
                detail(MistakeType.EXTRA_CHARACTER));

        TypingStatistics result = TypingStatisticsCalculator.calculate(7, alignment, 60);

        assertEquals(7, result.totalCharactersTyped());
        assertEquals(5, result.correctCharacters());
        assertEquals(1, result.wrongCharacters());
        assertEquals(1, result.missingCharacters());
        assertEquals(1, result.extraCharacters());
        assertEquals(3, result.netMistakes());
        assertEquals(60, result.durationInSeconds());
        assertEquals(7.0, result.charactersPerMinute());
        assertEquals(1.4, result.grossWpm());
        assertEquals(1.0, result.correctWpm());
        assertEquals(62.5, result.accuracy());
    }

    @Test
    void grossWpmAndCpmScaleUsingExactElapsedMinutes() {
        TypingStatistics result = TypingStatisticsCalculator.calculate(
                250, repeated(MistakeType.MATCH, 250), 30);

        assertEquals(500.0, result.charactersPerMinute());
        assertEquals(100.0, result.grossWpm());
        assertEquals(100.0, result.correctWpm());
        assertEquals(100.0, result.accuracy());
    }

    @Test
    void correctWpmCannotExceedGrossWpm() {
        List<ComparisonDetailResponse> alignment = List.of(
                detail(MistakeType.MATCH),
                detail(MistakeType.MATCH),
                detail(MistakeType.WRONG_CHARACTER),
                detail(MistakeType.EXTRA_CHARACTER),
                detail(MistakeType.MISSING_CHARACTER));
        TypingStatistics result = TypingStatisticsCalculator.calculate(4, alignment, 60);

        assertEquals(0.8, result.grossWpm());
        assertEquals(0.4, result.correctWpm());
        assertTrue(result.correctWpm() <= result.grossWpm());
    }

    @Test
    void emptyAttemptHasZeroSpeedAndPerfectEmptyAlignmentAccuracy() {
        TypingStatistics result = TypingStatisticsCalculator.calculate(0, List.of(), 300);

        assertEquals(0.0, result.charactersPerMinute());
        assertEquals(0.0, result.grossWpm());
        assertEquals(0.0, result.correctWpm());
        assertEquals(100.0, result.accuracy());
        assertEquals(0, result.netMistakes());
    }

    @Test
    void allErrorsProduceZeroCorrectWpmAndZeroAccuracy() {
        List<ComparisonDetailResponse> alignment = List.of(
                detail(MistakeType.WRONG_CHARACTER),
                detail(MistakeType.MISSING_CHARACTER),
                detail(MistakeType.EXTRA_CHARACTER));

        TypingStatistics result = TypingStatisticsCalculator.calculate(2, alignment, 60);

        assertEquals(0.0, result.correctWpm());
        assertEquals(0.0, result.accuracy());
        assertEquals(3, result.netMistakes());
    }

    @Test
    void resultsUseProfessionalTwoDecimalRounding() {
        TypingStatistics result = TypingStatisticsCalculator.calculate(
                2, List.of(detail(MistakeType.MATCH), detail(MistakeType.WRONG_CHARACTER)), 90);

        assertEquals(1.33, result.charactersPerMinute());
        assertEquals(0.27, result.grossWpm());
        assertEquals(0.13, result.correctWpm());
        assertEquals(50.0, result.accuracy());
    }

    @Test
    void rejectsZeroAndNegativeDurations() {
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                () -> TypingStatisticsCalculator.calculate(1, List.of(), 0));
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> TypingStatisticsCalculator.calculate(1, List.of(), -1));

        assertEquals("durationInSeconds must be positive", zero.getMessage());
        assertEquals("durationInSeconds must be positive", negative.getMessage());
    }

    @Test
    void rejectsNegativeTypedCharacterCount() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TypingStatisticsCalculator.calculate(-1, List.of(), 60));

        assertEquals("totalCharactersTyped must not be negative", exception.getMessage());
    }

    @Test
    void rejectsTypedCharacterCountThatDisagreesWithAlignment() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TypingStatisticsCalculator.calculate(
                        1, repeated(MistakeType.MATCH, 2), 60));

        assertEquals("totalCharactersTyped must match the typed characters in the alignment",
                exception.getMessage());
    }

    @Test
    void allPublishedRatesAreFiniteAndAccuracyIsBounded() {
        TypingStatistics result = TypingStatisticsCalculator.calculate(
                6_000, repeated(MistakeType.MATCH, 6_000), 1);

        assertTrue(Double.isFinite(result.charactersPerMinute()));
        assertTrue(Double.isFinite(result.grossWpm()));
        assertTrue(Double.isFinite(result.correctWpm()));
        assertTrue(result.accuracy() >= 0.0 && result.accuracy() <= 100.0);
    }

    private ComparisonDetailResponse detail(MistakeType type) {
        return new ComparisonDetailResponse(0, 0, null, null, type);
    }

    private List<ComparisonDetailResponse> repeated(MistakeType type, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(ignored -> detail(type))
                .toList();
    }
}
