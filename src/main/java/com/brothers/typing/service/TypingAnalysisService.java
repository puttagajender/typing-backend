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

    private final RecommendationService recommendationService;
    private final WeakKeyAnalysisService weakKeyAnalysisService;

    public TypingAnalysisService(
            RecommendationService recommendationService,
            WeakKeyAnalysisService weakKeyAnalysisService) {
        this.recommendationService = recommendationService;
        this.weakKeyAnalysisService = weakKeyAnalysisService;
    }

    public TypingAnalysisResponse analyze(TypingAnalysisRequest request) {
        long durationInSeconds = calculateDurationInSeconds(request);

        List<ComparisonDetailResponse> comparisonDetails = align(
                request.originalText(), request.typedText());
        List<MistakeDetailResponse> mistakeDetails = comparisonDetails.stream()
                .filter(detail -> detail.mistakeType() != MistakeType.MATCH)
                .map(this::toMistakeDetail)
                .toList();
        TypingStatistics statistics = TypingStatisticsCalculator.calculate(
                request.typedText().length(), comparisonDetails, durationInSeconds);
        Recommendation recommendation = recommendationService.recommend(new RecommendationInput(
                statistics.correctWpm(),
                statistics.grossWpm(),
                statistics.accuracy(),
                statistics.netMistakes()));
        WeakKeyAnalysis weakKeyAnalysis = weakKeyAnalysisService.analyze(comparisonDetails);

        TypingAnalysisResponse response = new TypingAnalysisResponse(
                statistics.correctWpm(),
                statistics.grossWpm(),
                statistics.correctWpm(),
                statistics.accuracy(),
                statistics.durationInSeconds(),
                statistics.netMistakes(),
                statistics.wrongCharacters(),
                statistics.missingCharacters(),
                statistics.extraCharacters(),
                mistakeDetails,
                comparisonDetails,
                recommendation.typingLevel(),
                recommendation.typingLevel().getDisplayName(),
                recommendation.recommendedDifficulty(),
                recommendation.recommendedCategory(),
                recommendation.recommendedDuration(),
                recommendation.recommendationReason(),
                weakKeyAnalysis.weakKeys(),
                weakKeyAnalysis.weakKeySummary(),
                weakKeyAnalysis.suggestedPracticeWords()
        );
        log.info("Typing analysis completed: durationSeconds={}, grossWpm={}, correctWpm={}, "
                        + "accuracy={}, mistakes={}, recommendation={}/{}/{}s",
                statistics.durationInSeconds(), statistics.grossWpm(), statistics.correctWpm(),
                statistics.accuracy(), statistics.netMistakes(), recommendation.typingLevel(),
                recommendation.recommendedDifficulty(), recommendation.recommendedDuration());
        return response;
    }

    /**
     * Calculates the positive whole-second duration represented by the API timestamps.
     * Sub-second attempts are rejected because the existing response contract exposes
     * duration as whole seconds and WPM cannot safely divide by zero.
     */
    private long calculateDurationInSeconds(TypingAnalysisRequest request) {
        long seconds = Duration.between(request.startedAt(), request.completedAt()).getSeconds();
        if (seconds <= 0) {
            throw new IllegalArgumentException("completedAt must be after startedAt");
        }
        return seconds;
    }

    private MistakeDetailResponse toMistakeDetail(ComparisonDetailResponse detail) {
        return new MistakeDetailResponse(
                detail.originalPosition(),
                detail.expectedCharacter(),
                detail.typedCharacter(),
                detail.mistakeType());
    }

    private List<ComparisonDetailResponse> align(String original, String typed) {
        if (original.equals(typed)) {
            List<ComparisonDetailResponse> exactMatches = new ArrayList<>(original.length());
            for (int index = 0; index < original.length(); index++) {
                char character = original.charAt(index);
                exactMatches.add(new ComparisonDetailResponse(
                        index, index, character, character, MistakeType.MATCH));
            }
            return List.copyOf(exactMatches);
        }

        short[][] distances = buildDistanceMatrix(original, typed);
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

    private short[][] buildDistanceMatrix(String original, String typed) {
        short[][] distances = new short[original.length() + 1][typed.length() + 1];

        for (int i = original.length(); i >= 0; i--) {
            distances[i][typed.length()] = (short) (original.length() - i);
        }
        for (int j = typed.length(); j >= 0; j--) {
            distances[original.length()][j] = (short) (typed.length() - j);
        }

        for (int i = original.length() - 1; i >= 0; i--) {
            for (int j = typed.length() - 1; j >= 0; j--) {
                int substitutionCost = original.charAt(i) == typed.charAt(j) ? 0 : 1;
                distances[i][j] = (short) Math.min(
                        Math.min(distances[i + 1][j] + 1, distances[i][j + 1] + 1),
                        distances[i + 1][j + 1] + substitutionCost
                );
            }
        }
        return distances;
    }

}
