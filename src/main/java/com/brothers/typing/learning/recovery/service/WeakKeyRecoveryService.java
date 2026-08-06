package com.brothers.typing.learning.recovery.service;

import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.recovery.dto.RecoveryExerciseResponse;
import com.brothers.typing.learning.recovery.dto.RecoveryExerciseType;
import com.brothers.typing.learning.recovery.dto.WeakKeyPriority;
import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryRequest;
import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryResponse;
import com.brothers.typing.learning.recovery.dto.WeakKeyResponse;
import com.brothers.typing.learning.service.LessonWordDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

@Service
public class WeakKeyRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(WeakKeyRecoveryService.class);
    private static final List<RecoveryExerciseType> PROGRESSION = List.of(
            RecoveryExerciseType.SINGLE_KEY,
            RecoveryExerciseType.HOME_MOVEMENT_PAIR,
            RecoveryExerciseType.NEIGHBOUR_PAIR,
            RecoveryExerciseType.SHORT_PATTERN,
            RecoveryExerciseType.WORD,
            RecoveryExerciseType.MIXED_REVIEW);

    private final WeakKeyAnalysisService analysisService;
    private final WeakKeyRecoveryProperties properties;
    private final HomeRowFingerMap fingerMap;
    private final LessonWordDictionary dictionary;
    private final LongSupplier seedSupplier;

    @Autowired
    public WeakKeyRecoveryService(
            WeakKeyAnalysisService analysisService,
            WeakKeyRecoveryProperties properties,
            HomeRowFingerMap fingerMap,
            LessonWordDictionary dictionary) {
        this(analysisService, properties, fingerMap, dictionary, () -> 0L);
    }

    WeakKeyRecoveryService(
            WeakKeyAnalysisService analysisService,
            WeakKeyRecoveryProperties properties,
            HomeRowFingerMap fingerMap,
            LessonWordDictionary dictionary,
            LongSupplier seedSupplier) {
        this.analysisService = analysisService;
        this.properties = properties;
        this.fingerMap = fingerMap;
        this.dictionary = dictionary;
        this.seedSupplier = seedSupplier;
    }

    public WeakKeyRecoveryResponse generate(WeakKeyRecoveryRequest request) {
        GenerationContext context = validate(request);
        List<WeakKeyResponse> analysed = analysisService.analyze(
                request.keyPerformance(), context.learnedKeys());
        List<WeakKeyResponse> selected = analysed.stream()
                .filter(key -> key.priority() == WeakKeyPriority.HIGH
                        || key.priority() == WeakKeyPriority.MEDIUM)
                .filter(key -> !recentlyCompleted(request, key.key()))
                .limit(Math.max(0, Math.min(3, properties.getMaximumWeakKeys())))
                .toList();

        if (selected.isEmpty()) {
            log.info("Weak-key recovery evaluated: lessonId={}, analysedKeyCount={}, "
                            + "detectedWeakKeyCount=0, exerciseCount=0, estimatedDurationSeconds=0",
                    request.lessonId(), request.keyPerformance().size());
            return new WeakKeyRecoveryResponse(
                    request.lessonId(), false, List.of(),
                    "No focused recovery is needed. Continue normal practice.", List.of());
        }

        List<RecoveryExerciseResponse> exercises = generateExercises(request, selected, context);
        if (exercises.isEmpty()) {
            return new WeakKeyRecoveryResponse(
                    request.lessonId(), false, List.of(),
                    "Focused recovery was completed recently. Continue normal practice.", List.of());
        }
        int estimatedDuration = exercises.stream()
                .mapToInt(RecoveryExerciseResponse::estimatedDurationSeconds).sum();
        String selectedKeys = selected.stream().map(WeakKeyResponse::key)
                .collect(Collectors.joining(","));
        log.info("Weak-key recovery generated: lessonId={}, analysedKeyCount={}, "
                        + "detectedWeakKeyCount={}, selectedKeys={}, exerciseCount={}, "
                        + "estimatedDurationSeconds={}",
                request.lessonId(), request.keyPerformance().size(), selected.size(), selectedKeys,
                exercises.size(), estimatedDuration);

        return new WeakKeyRecoveryResponse(
                request.lessonId(), true, selected, reason(selected), exercises);
    }

    private GenerationContext validate(WeakKeyRecoveryRequest request) {
        if (!fingerMap.supportsLesson(request.lessonId())
                || !dictionary.supports(request.lessonId())) {
            throw new WeakKeyRecoveryException("Unsupported lesson");
        }
        if (request.requestedDurationMinutes() < 1
                || request.requestedDurationMinutes() > properties.getMaximumDurationMinutes()) {
            throw new WeakKeyRecoveryException(
                    "requestedDurationMinutes must be between 1 and "
                            + properties.getMaximumDurationMinutes());
        }
        Set<String> learned = new LinkedHashSet<>(request.learnedKeys());
        if (!fingerMap.supportedKeys().containsAll(learned)) {
            throw new WeakKeyRecoveryException(
                    "learnedKeys contains a key not supported by the lesson");
        }
        return new GenerationContext(Set.copyOf(learned), validWords(request.lessonId(), learned));
    }

    private List<RecoveryExerciseResponse> generateExercises(
            WeakKeyRecoveryRequest request,
            List<WeakKeyResponse> selected,
            GenerationContext context) {
        int targetSeconds = request.requestedDurationMinutes() * 60;
        Set<String> completedIds = Set.copyOf(request.completedExerciseIds());
        Set<String> contents = new LinkedHashSet<>();
        List<RecoveryExerciseResponse> result = new ArrayList<>();
        Map<String, Integer> phases = new HashMap<>();
        long generationSeed = seedSupplier.getAsLong() ^ request.hashCode();
        int estimatedSeconds = 0;

        for (int sequence = 0; sequence < 100 && estimatedSeconds < targetSeconds; sequence++) {
            WeakKeyResponse weakKey = selected.get(weightedKeyIndex(sequence, selected.size()));
            int phase = phases.merge(weakKey.key(), 1, Integer::sum) - 1;
            RecoveryExerciseType requestedType = PROGRESSION.get(phase % PROGRESSION.size());
            Candidate candidate = candidate(
                    requestedType, weakKey.key(), phase, context, generationSeed);
            if (candidate == null) continue;
            String id = stableId(request.lessonId(), weakKey.key(), candidate.type(), phase);
            if (completedIds.contains(id) || !contents.add(candidate.content())) continue;
            validateContent(candidate.content(), context.learnedKeys());
            int duration = estimatedDuration(candidate.type());
            if (!result.isEmpty() && estimatedSeconds + duration > targetSeconds + 15) break;
            result.add(new RecoveryExerciseResponse(
                    id, candidate.type(), candidate.content(), List.of(weakKey.key()), duration));
            estimatedSeconds += duration;
        }
        return List.copyOf(result);
    }

    private Candidate candidate(
            RecoveryExerciseType type,
            String weakKey,
            int variant,
            GenerationContext context,
            long generationSeed) {
        List<String> neighbours = fingerMap.neighbours(weakKey, context.learnedKeys());
        return switch (type) {
            case SINGLE_KEY -> new Candidate(type, repeat(weakKey, 5 + variant % 4));
            case HOME_MOVEMENT_PAIR -> new Candidate(type,
                    pairContent(weakKey, neighbours, variant, false));
            case NEIGHBOUR_PAIR -> new Candidate(type,
                    pairContent(weakKey, neighbours, variant, true));
            case SHORT_PATTERN -> new Candidate(type,
                    shortPattern(weakKey, neighbours, variant, generationSeed));
            case WORD -> {
                List<String> words = context.words().stream()
                        .filter(word -> word.contains(weakKey)).toList();
                yield words.isEmpty() ? new Candidate(
                        RecoveryExerciseType.SHORT_PATTERN,
                        shortPattern(weakKey, neighbours, variant, generationSeed))
                        : new Candidate(type, repeat(words.get(variant % words.size()), 2 + variant % 3));
            }
            case MIXED_REVIEW -> new Candidate(type,
                    mixedReview(weakKey, neighbours, context.words(), variant));
        };
    }

    private String pairContent(
            String weakKey, List<String> neighbours, int variant, boolean includeAll) {
        if (neighbours.isEmpty()) return repeat(weakKey, 4 + variant % 4);
        List<String> selected = includeAll
                ? neighbours : List.of(neighbours.get(variant % neighbours.size()));
        return selected.stream()
                .map(key -> key + weakKey + " " + weakKey + key)
                .collect(Collectors.joining(" "));
    }

    private String shortPattern(
            String weakKey, List<String> neighbours, int variant, long generationSeed) {
        String other = neighbours.isEmpty() ? weakKey
                : neighbours.get(Math.floorMod(generationSeed + variant, neighbours.size()));
        String pattern = variant % 2 == 0
                ? weakKey + other + weakKey : other + weakKey + other;
        return repeat(pattern, 2 + variant % 3);
    }

    private String mixedReview(
            String weakKey, List<String> neighbours, List<String> words, int variant) {
        List<String> validWeakWords = words.stream().filter(word -> word.contains(weakKey)).toList();
        if (!validWeakWords.isEmpty()) {
            String first = validWeakWords.get(variant % validWeakWords.size());
            String second = validWeakWords.get((variant + 1) % validWeakWords.size());
            return first + " " + second + " " + first;
        }
        return pairContent(weakKey, neighbours, variant, true) + " "
                + shortPattern(weakKey, neighbours, variant, variant);
    }

    private int weightedKeyIndex(int sequence, int keyCount) {
        if (keyCount == 1) return 0;
        if (keyCount == 2) return sequence % 3 == 1 ? 1 : 0;
        return switch (sequence % 6) {
            case 2, 4 -> 1;
            case 5 -> 2;
            default -> 0;
        };
    }

    private boolean recentlyCompleted(WeakKeyRecoveryRequest request, String key) {
        String finalReviewId = stableId(
                request.lessonId(), key, RecoveryExerciseType.MIXED_REVIEW, 5);
        return request.completedExerciseIds().contains(finalReviewId);
    }

    private List<String> validWords(String lessonId, Set<String> learnedKeys) {
        return dictionary.wordsFor(lessonId).stream()
                .filter(word -> isAllowed(word, learnedKeys)).toList();
    }

    private void validateContent(String content, Set<String> learnedKeys) {
        if (!isAllowed(content, learnedKeys)) {
            throw new IllegalStateException("Generated recovery exercise contains an untaught key");
        }
    }

    private boolean isAllowed(String content, Set<String> learnedKeys) {
        return content.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .allMatch(character -> character.equals(" ") || learnedKeys.contains(character));
    }

    private int estimatedDuration(RecoveryExerciseType type) {
        return switch (type) {
            case SINGLE_KEY -> 20;
            case HOME_MOVEMENT_PAIR, NEIGHBOUR_PAIR, SHORT_PATTERN -> 25;
            case WORD, MIXED_REVIEW -> 30;
        };
    }

    private String stableId(
            String lessonId, String key, RecoveryExerciseType type, int variant) {
        return UUID.nameUUIDFromBytes(
                (lessonId + "|recovery|" + key + "|" + type + "|" + variant)
                        .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String repeat(String value, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(ignored -> value).collect(Collectors.joining(" "));
    }

    private String reason(List<WeakKeyResponse> selected) {
        if (selected.size() == 1) {
            return "The " + selected.get(0).key().toUpperCase(Locale.ROOT)
                    + " key needs focused practice before continuing.";
        }
        return "Focused practice is recommended for " + selected.stream()
                .map(key -> key.key().toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", ")) + " before continuing.";
    }

    private record GenerationContext(Set<String> learnedKeys, List<String> words) { }
    private record Candidate(RecoveryExerciseType type, String content) { }
}
