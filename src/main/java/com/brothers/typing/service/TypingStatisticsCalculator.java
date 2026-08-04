package com.brothers.typing.service;

import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeType;

import java.util.List;

/** Calculates typing-test statistics from the submitted text and its alignment. */
final class TypingStatisticsCalculator {

    private static final double CHARACTERS_PER_STANDARD_WORD = 5.0;
    private static final double SECONDS_PER_MINUTE = 60.0;

    private TypingStatisticsCalculator() {
    }

    /**
     * Calculates a mutually consistent set of typing statistics.
     *
     * <p>Accuracy uses every alignment outcome as its denominator. Consequently,
     * substitutions, omissions, and additions each reduce accuracy, including text
     * left missing from an incomplete final passage.</p>
     *
     * @param totalCharactersTyped exact submitted-text length, without trimming
     * @param comparisonDetails complete sequence-alignment result
     * @param durationInSeconds positive elapsed whole seconds
     * @return rounded, bounded statistics
     */
    static TypingStatistics calculate(
            int totalCharactersTyped,
            List<ComparisonDetailResponse> comparisonDetails,
            long durationInSeconds) {
        if (totalCharactersTyped < 0) {
            throw new IllegalArgumentException("totalCharactersTyped must not be negative");
        }
        if (durationInSeconds <= 0) {
            throw new IllegalArgumentException("durationInSeconds must be positive");
        }

        int correctCharacters = count(comparisonDetails, MistakeType.MATCH);
        int wrongCharacters = count(comparisonDetails, MistakeType.WRONG_CHARACTER);
        int missingCharacters = count(comparisonDetails, MistakeType.MISSING_CHARACTER);
        int extraCharacters = count(comparisonDetails, MistakeType.EXTRA_CHARACTER);

        int alignedTypedCharacters = Math.addExact(
                Math.addExact(correctCharacters, wrongCharacters), extraCharacters);
        if (alignedTypedCharacters != totalCharactersTyped) {
            throw new IllegalArgumentException(
                    "totalCharactersTyped must match the typed characters in the alignment");
        }

        int netMistakes = calculateNetMistakes(
                wrongCharacters, missingCharacters, extraCharacters);

        double minutes = durationInSeconds / SECONDS_PER_MINUTE;
        double charactersPerMinute = calculateCharactersPerMinute(totalCharactersTyped, minutes);
        double grossWpm = calculateGrossWpm(charactersPerMinute);
        double correctWpm = calculateCorrectWpm(correctCharacters, minutes, grossWpm);
        double accuracy = calculateAccuracy(correctCharacters, comparisonDetails.size());

        return new TypingStatistics(
                totalCharactersTyped,
                correctCharacters,
                wrongCharacters,
                missingCharacters,
                extraCharacters,
                netMistakes,
                durationInSeconds,
                roundToTwoDecimals(charactersPerMinute),
                roundToTwoDecimals(grossWpm),
                roundToTwoDecimals(correctWpm),
                roundToTwoDecimals(accuracy));
    }

    /**
     * Calculates net mistakes as wrong + missing + extra alignment outcomes.
     * Exact arithmetic prevents silent integer wraparound.
     */
    private static int calculateNetMistakes(int wrong, int missing, int extra) {
        return Math.addExact(Math.addExact(wrong, missing), extra);
    }

    /** Calculates CPM as all submitted characters divided by elapsed minutes. */
    private static double calculateCharactersPerMinute(int totalCharactersTyped, double minutes) {
        return totalCharactersTyped / minutes;
    }

    /** Calculates Gross WPM as CPM divided by the standard five characters per word. */
    private static double calculateGrossWpm(double charactersPerMinute) {
        return charactersPerMinute / CHARACTERS_PER_STANDARD_WORD;
    }

    /**
     * Calculates Correct WPM from exact character matches divided by five and elapsed
     * minutes. The result is capped at Gross WPM as a defensive invariant.
     */
    private static double calculateCorrectWpm(
            int correctCharacters, double minutes, double grossWpm) {
        double matchedCharacterWpm =
                (correctCharacters / CHARACTERS_PER_STANDARD_WORD) / minutes;
        return Math.min(grossWpm, matchedCharacterWpm);
    }

    /**
     * Calculates accuracy as correct alignment outcomes divided by all alignment
     * outcomes, multiplied by 100 and clamped to the inclusive range 0–100.
     */
    private static double calculateAccuracy(int correctCharacters, int alignmentLength) {
        double accuracy = alignmentLength == 0
                ? 100.0
                : (double) correctCharacters / alignmentLength * 100.0;
        return clamp(accuracy, 0.0, 100.0);
    }

    private static int count(List<ComparisonDetailResponse> details, MistakeType type) {
        return Math.toIntExact(details.stream()
                .filter(detail -> detail.mistakeType() == type)
                .count());
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
