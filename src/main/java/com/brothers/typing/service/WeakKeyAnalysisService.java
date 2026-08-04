package com.brothers.typing.service;

import com.brothers.typing.config.WeakKeyProperties;
import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeType;
import com.brothers.typing.dto.WeakKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeakKeyAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(WeakKeyAnalysisService.class);

    private final WeakKeyProperties properties;

    public WeakKeyAnalysisService(WeakKeyProperties properties) {
        this.properties = properties;
    }

    public WeakKeyAnalysis analyze(List<ComparisonDetailResponse> comparisons) {
        Map<Character, EnumMap<MistakeType, Integer>> mistakeCounts = countMistakes(comparisons);
        int totalMistakes = mistakeCounts.values().stream()
                .mapToInt(this::totalCount)
                .sum();

        List<WeakKeyResponse> weakKeys = mistakeCounts.entrySet().stream()
                .filter(entry -> includeCharacter(entry.getKey(), totalCount(entry.getValue())))
                .map(entry -> toResponse(entry.getKey(), entry.getValue(), totalMistakes))
                .sorted(Comparator.comparingInt(WeakKeyResponse::mistakeCount).reversed()
                        .thenComparing(WeakKeyResponse::character))
                .limit(Math.max(0, Math.min(5, properties.getMaximumWeakKeys())))
                .toList();

        String summary = createSummary(weakKeys);
        Map<String, List<String>> suggestedWords = suggestedWords(weakKeys);
        log.info("Weak-key count: {}", weakKeys.size());
        log.info("Top weak keys: {}", weakKeys.stream()
                .map(WeakKeyResponse::character)
                .collect(Collectors.joining(",")));
        return new WeakKeyAnalysis(weakKeys, summary, suggestedWords);
    }

    private Map<Character, EnumMap<MistakeType, Integer>> countMistakes(
            List<ComparisonDetailResponse> comparisons) {
        Map<Character, EnumMap<MistakeType, Integer>> counts = new LinkedHashMap<>();
        for (ComparisonDetailResponse comparison : comparisons) {
            Character character = mistakeCharacter(comparison);
            if (character == null) {
                continue;
            }
            counts.computeIfAbsent(normalize(character), ignored -> new EnumMap<>(MistakeType.class))
                    .merge(comparison.mistakeType(), 1, Integer::sum);
        }
        return counts;
    }

    private Character mistakeCharacter(ComparisonDetailResponse comparison) {
        return switch (comparison.mistakeType()) {
            case WRONG_CHARACTER, MISSING_CHARACTER -> comparison.expectedCharacter();
            case EXTRA_CHARACTER -> comparison.typedCharacter();
            case MATCH -> null;
        };
    }

    private Character normalize(Character character) {
        return Character.toUpperCase(character);
    }

    private boolean includeCharacter(Character character, int mistakeCount) {
        return character != ' ' || mistakeCount >= properties.getMinimumSpaceMistakes();
    }

    private WeakKeyResponse toResponse(
            Character character,
            EnumMap<MistakeType, Integer> counts,
            int totalMistakes) {
        int characterMistakes = totalCount(counts);
        double percentage = totalMistakes == 0
                ? 0
                : (double) characterMistakes / totalMistakes * 100;
        return new WeakKeyResponse(
                displayCharacter(character),
                characterMistakes,
                roundToTwoDecimals(percentage),
                dominantType(counts));
    }

    private MistakeType dominantType(EnumMap<MistakeType, Integer> counts) {
        return counts.entrySet().stream()
                .max(Map.Entry.<MistakeType, Integer>comparingByValue()
                        .thenComparing(entry -> entry.getKey().ordinal(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(MistakeType.WRONG_CHARACTER);
    }

    private int totalCount(EnumMap<MistakeType, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String displayCharacter(Character character) {
        return character == ' ' ? "SPACE" : character.toString();
    }

    private String createSummary(List<WeakKeyResponse> weakKeys) {
        if (weakKeys.isEmpty()) {
            return properties.getNoWeakKeysSummary();
        }

        List<String> characters = weakKeys.stream().map(WeakKeyResponse::character).toList();
        String characterList;
        if (characters.size() == 1) {
            characterList = characters.get(0);
        } else {
            characterList = String.join(", ", characters.subList(0, characters.size() - 1))
                    + " and " + characters.get(characters.size() - 1);
        }
        return "You frequently struggle with " + characterList + ".";
    }

    private Map<String, List<String>> suggestedWords(List<WeakKeyResponse> weakKeys) {
        Map<String, List<String>> suggestions = new LinkedHashMap<>();
        for (WeakKeyResponse weakKey : weakKeys) {
            String lookupKey = weakKey.character().toLowerCase(Locale.ROOT);
            List<String> words = properties.getPracticeWords()
                    .getOrDefault(lookupKey, List.of());
            suggestions.put(weakKey.character(), List.copyOf(words));
        }
        return Map.copyOf(suggestions);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
