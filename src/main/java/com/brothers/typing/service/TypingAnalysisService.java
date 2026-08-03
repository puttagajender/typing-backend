package com.brothers.typing.service;

import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeDetailResponse;
import com.brothers.typing.dto.MistakeType;
import com.brothers.typing.dto.TypingAnalysisRequest;
import com.brothers.typing.dto.TypingAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class TypingAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(TypingAnalysisService.class);

    public TypingAnalysisResponse analyze(TypingAnalysisRequest request) {
        log.info("Typing analysis started");
        long durationInSeconds = Duration.between(request.startedAt(), request.completedAt()).getSeconds();
        if (durationInSeconds <= 0) {
            throw new IllegalArgumentException("completedAt must be after startedAt");
        }
        log.info("Duration calculated: seconds={}", durationInSeconds);

        List<ComparisonDetailResponse> comparisonDetails = align(
                request.originalText(), request.typedText());
        List<MistakeDetailResponse> mistakeDetails = comparisonDetails.stream()
                .filter(detail -> detail.mistakeType() != MistakeType.MATCH)
                .map(this::toMistakeDetail)
                .toList();
        int mistakeCount = mistakeDetails.size();
        int wrongCharacterCount = countMistakes(mistakeDetails, MistakeType.WRONG_CHARACTER);
        int missingCharacterCount = countMistakes(mistakeDetails, MistakeType.MISSING_CHARACTER);
        int extraCharacterCount = countMistakes(mistakeDetails, MistakeType.EXTRA_CHARACTER);
        log.info("Mistake detection completed: count={}", mistakeCount);

        double minutes = durationInSeconds / 60.0;
        double grossWpm = (request.typedText().length() / 5.0) / minutes;
        int comparisonLength = comparisonDetails.size();
        int correctCharacterCount = countComparisons(comparisonDetails, MistakeType.MATCH);
        double correctWpm = (correctCharacterCount / 5.0) / minutes;
        log.info("WPM calculated: gross={}, correct={}",
                roundToTwoDecimals(grossWpm), roundToTwoDecimals(correctWpm));

        double accuracy = comparisonLength == 0
                ? 100.0
                : (double) correctCharacterCount / comparisonLength * 100.0;
        accuracy = Math.max(0.0, Math.min(100.0, accuracy));
        log.info("Accuracy calculated: value={}", roundToTwoDecimals(accuracy));

        TypingAnalysisResponse response = new TypingAnalysisResponse(
                roundToTwoDecimals(correctWpm),
                roundToTwoDecimals(grossWpm),
                roundToTwoDecimals(correctWpm),
                roundToTwoDecimals(accuracy),
                durationInSeconds,
                mistakeCount,
                wrongCharacterCount,
                missingCharacterCount,
                extraCharacterCount,
                mistakeDetails,
                comparisonDetails
        );
        log.info("Typing analysis response generated");
        return response;
    }

    private int countMistakes(List<MistakeDetailResponse> mistakes, MistakeType type) {
        return (int) mistakes.stream()
                .filter(mistake -> mistake.mistakeType() == type)
                .count();
    }

    private int countComparisons(List<ComparisonDetailResponse> comparisons, MistakeType type) {
        return (int) comparisons.stream()
                .filter(comparison -> comparison.mistakeType() == type)
                .count();
    }

    private MistakeDetailResponse toMistakeDetail(ComparisonDetailResponse detail) {
        return new MistakeDetailResponse(
                detail.originalPosition(),
                detail.expectedCharacter(),
                detail.typedCharacter(),
                detail.mistakeType());
    }

    private List<ComparisonDetailResponse> align(String original, String typed) {
        int[][] distances = buildDistanceMatrix(original, typed);
        List<ComparisonDetailResponse> comparisons = new ArrayList<>();
        int originalIndex = 0;
        int typedIndex = 0;

        while (originalIndex < original.length() || typedIndex < typed.length()) {
            if (originalIndex < original.length()
                    && typedIndex < typed.length()
                    && original.charAt(originalIndex) == typed.charAt(typedIndex)) {
                comparisons.add(new ComparisonDetailResponse(
                        originalIndex,
                        typedIndex,
                        original.charAt(originalIndex),
                        typed.charAt(typedIndex),
                        MistakeType.MATCH));
                originalIndex++;
                typedIndex++;
            } else if (originalIndex < original.length()
                    && typedIndex < typed.length()
                    && distances[originalIndex][typedIndex]
                    == 1 + distances[originalIndex + 1][typedIndex + 1]) {
                comparisons.add(new ComparisonDetailResponse(
                        originalIndex,
                        typedIndex,
                        original.charAt(originalIndex),
                        typed.charAt(typedIndex),
                        MistakeType.WRONG_CHARACTER));
                originalIndex++;
                typedIndex++;
            } else if (originalIndex < original.length()
                    && distances[originalIndex][typedIndex]
                    == 1 + distances[originalIndex + 1][typedIndex]) {
                comparisons.add(new ComparisonDetailResponse(
                        originalIndex,
                        typedIndex,
                        original.charAt(originalIndex),
                        null,
                        MistakeType.MISSING_CHARACTER));
                originalIndex++;
            } else {
                comparisons.add(new ComparisonDetailResponse(
                        originalIndex,
                        typedIndex,
                        null,
                        typed.charAt(typedIndex),
                        MistakeType.EXTRA_CHARACTER));
                typedIndex++;
            }
        }

        return List.copyOf(comparisons);
    }

    private int[][] buildDistanceMatrix(String original, String typed) {
        int[][] distances = new int[original.length() + 1][typed.length() + 1];

        for (int i = original.length(); i >= 0; i--) {
            distances[i][typed.length()] = original.length() - i;
        }
        for (int j = typed.length(); j >= 0; j--) {
            distances[original.length()][j] = typed.length() - j;
        }

        for (int i = original.length() - 1; i >= 0; i--) {
            for (int j = typed.length() - 1; j >= 0; j--) {
                int substitutionCost = original.charAt(i) == typed.charAt(j) ? 0 : 1;
                distances[i][j] = Math.min(
                        Math.min(distances[i + 1][j] + 1, distances[i][j + 1] + 1),
                        distances[i + 1][j + 1] + substitutionCost
                );
            }
        }
        return distances;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
