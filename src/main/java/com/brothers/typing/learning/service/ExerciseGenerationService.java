package com.brothers.typing.learning.service;

import com.brothers.typing.learning.dto.ExerciseGenerationRequest;
import com.brothers.typing.learning.dto.ExerciseGenerationResponse;
import com.brothers.typing.learning.dto.ExerciseType;
import com.brothers.typing.learning.dto.GeneratedExerciseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

@Service
public class ExerciseGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseGenerationService.class);
    private static final int MAX_CANDIDATE_ATTEMPTS = 500;
    private static final Map<String, Set<String>> LESSON_KEYS = Map.of(
            "HOME_ROW_1", Set.of("a", "s", "d", "f", "j", "k", "l", ";"));

    private final LessonWordDictionary dictionary;
    private final SessionPlanningService planningService;
    private final LongSupplier seedSupplier;

    @Autowired
    public ExerciseGenerationService(
            LessonWordDictionary dictionary,
            SessionPlanningService planningService) {
        this(dictionary, planningService, () -> 0L);
    }

    ExerciseGenerationService(
            LessonWordDictionary dictionary,
            SessionPlanningService planningService,
            LongSupplier seedSupplier) {
        this.dictionary = dictionary;
        this.planningService = planningService;
        this.seedSupplier = seedSupplier;
    }

    public ExerciseGenerationResponse generate(ExerciseGenerationRequest request) {
        GenerationContext context = validateAndCreateContext(request);
        List<ExerciseType> plan = planningService.plan(
                request.exerciseType(), request.sessionDurationMinutes(),
                !context.words().isEmpty(), !context.weakKeys().isEmpty());
        int durationPerExercise = Math.max(
                15, request.sessionDurationMinutes() * 60 / Math.max(1, plan.size()));

        Set<String> contents = new LinkedHashSet<>();
        Set<String> previousIds = Set.copyOf(request.previousExerciseIds());
        List<GeneratedExerciseResponse> exercises = new ArrayList<>(plan.size());
        Map<ExerciseType, Integer> distribution = new LinkedHashMap<>();
        SplittableRandom random = new SplittableRandom(seedSupplier.getAsLong() ^ request.hashCode());

        int candidateNumber = 0;
        for (ExerciseType plannedType : plan) {
            GeneratedExerciseResponse exercise = null;
            for (int attempt = 0; attempt < MAX_CANDIDATE_ATTEMPTS && exercise == null; attempt++) {
                int sequence = candidateNumber++;
                ExerciseType actualType = availableType(plannedType, context);
                String content = contentFor(actualType, sequence, context, random);
                if (content == null || content.isBlank() || !contents.add(content)) {
                    continue;
                }
                validateContent(content, context.learnedKeys());
                String id = stableId(request.lessonId(), actualType, content);
                if (previousIds.contains(id)) {
                    contents.remove(content);
                    continue;
                }
                exercise = new GeneratedExerciseResponse(
                        id, actualType, content, targetKeys(content, context.learnedKeys()),
                        durationPerExercise);
            }
            if (exercise != null) {
                exercises.add(exercise);
                distribution.merge(exercise.type(), 1, Integer::sum);
            }
        }

        if (exercises.isEmpty()) {
            throw new ExerciseGenerationException("No usable lesson configuration is available");
        }

        log.info("Exercise session generated: lessonId={}, durationMinutes={}, exerciseCount={}, "
                        + "weakKeyCount={}, typeDistribution={}",
                request.lessonId(), request.sessionDurationMinutes(), exercises.size(),
                context.weakKeys().size(), distribution);
        return new ExerciseGenerationResponse(
                request.lessonId(), request.sessionDurationMinutes(), exercises.size(),
                List.copyOf(exercises));
    }

    private GenerationContext validateAndCreateContext(ExerciseGenerationRequest request) {
        if (request.sessionDurationMinutes() < 5 || request.sessionDurationMinutes() > 30) {
            throw new ExerciseGenerationException(
                    "sessionDurationMinutes must be between 5 and 30");
        }
        Set<String> configuredKeys = LESSON_KEYS.get(request.lessonId());
        if (configuredKeys == null || !dictionary.supports(request.lessonId())) {
            throw new ExerciseGenerationException("Unsupported lesson");
        }

        List<String> learned = distinct(request.learnedKeys());
        List<String> weak = distinct(request.weakKeys());
        if (!configuredKeys.containsAll(learned)) {
            throw new ExerciseGenerationException("learnedKeys contains a key not supported by the lesson");
        }
        if (!learned.containsAll(weak)) {
            throw new ExerciseGenerationException("Every weak key must exist in learnedKeys");
        }

        List<String> prioritizedKeys = new ArrayList<>(learned.size());
        prioritizedKeys.addAll(weak);
        learned.stream().filter(key -> !weak.contains(key)).forEach(prioritizedKeys::add);
        List<String> validWords = dictionary.wordsFor(request.lessonId()).stream()
                .filter(word -> isAllowed(word, Set.copyOf(learned)))
                .toList();
        return new GenerationContext(
                List.copyOf(learned), List.copyOf(weak), List.copyOf(prioritizedKeys), validWords);
    }

    private ExerciseType availableType(ExerciseType requested, GenerationContext context) {
        if ((requested == ExerciseType.WORD || requested == ExerciseType.PHRASE)
                && context.words().isEmpty()) {
            return ExerciseType.RANDOM_PATTERN;
        }
        if (requested == ExerciseType.WEAK_KEY_RECOVERY && context.weakKeys().isEmpty()) {
            return ExerciseType.KEY_PAIR;
        }
        return requested;
    }

    private String contentFor(
            ExerciseType type, int sequence, GenerationContext context, SplittableRandom random) {
        List<String> keys = context.prioritizedKeys();
        return switch (type) {
            case SINGLE_KEY -> repeat(keys.get(sequence % keys.size()), 3 + sequence % 50);
            case KEY_PAIR -> pair(keys, sequence);
            case MOVEMENT -> movement(keys, sequence);
            case RANDOM_PATTERN -> randomPattern(keys, sequence, random);
            case WORD -> word(context.words(), sequence);
            case PHRASE -> phrase(context.words(), sequence);
            case WEAK_KEY_RECOVERY -> weakRecovery(context, sequence);
            case MIXED -> throw new IllegalStateException("MIXED must be expanded by the session planner");
        };
    }

    private String pair(List<String> keys, int sequence) {
        String first = keys.get(sequence % keys.size());
        String second = keys.get((sequence / keys.size() + 1) % keys.size());
        String pair = first + second + " " + second + first;
        return repeat(pair, 1 + sequence % 12);
    }

    private String movement(List<String> keys, int sequence) {
        int length = Math.min(keys.size(), 4 + sequence % 3);
        List<String> selected = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            selected.add(keys.get((sequence + index) % keys.size()));
        }
        if (sequence % 2 == 1) {
            Collections.reverse(selected);
        }
        String pattern = String.join("", selected);
        return repeat(pattern, 1 + sequence % 10);
    }

    private String randomPattern(List<String> keys, int sequence, SplittableRandom random) {
        int length = 4 + sequence % 5;
        StringBuilder pattern = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            pattern.append(keys.get(random.nextInt(keys.size())));
        }
        String mirroredPattern = pattern + " " + new StringBuilder(pattern).reverse();
        return repeat(mirroredPattern, 1 + sequence % 10);
    }

    private String word(List<String> words, int sequence) {
        String word = words.get(sequence % words.size());
        return repeat(word, 2 + sequence % 40);
    }

    private String phrase(List<String> words, int sequence) {
        int count = Math.min(words.size(), 2 + sequence % 3);
        List<String> phrase = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            phrase.add(words.get((sequence + index) % words.size()));
        }
        return repeat(String.join(" ", phrase), 1 + sequence % 8);
    }

    private String weakRecovery(GenerationContext context, int sequence) {
        String weak = context.weakKeys().get(sequence % context.weakKeys().size());
        List<String> otherKeys = context.learnedKeys().stream()
                .filter(key -> !key.equals(weak)).toList();
        return switch (sequence % 5) {
            case 0 -> repeat(weak, 5);
            case 1, 2 -> {
                if (otherKeys.isEmpty()) {
                    yield repeat(weak, 3 + sequence);
                }
                String other = otherKeys.get(sequence % otherKeys.size());
                yield other + weak + " " + weak + other + " " + other + weak;
            }
            case 3 -> {
                String validWord = context.words().stream()
                        .filter(candidate -> candidate.contains(weak)).findFirst().orElse(null);
                yield validWord == null ? repeat(weak, 4) : repeat(validWord, 3);
            }
            default -> weak + " " + randomPattern(context.prioritizedKeys(), sequence,
                    new SplittableRandom(sequence));
        };
    }

    private String repeat(String value, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(ignored -> value)
                .collect(Collectors.joining(" "));
    }

    private List<String> targetKeys(String content, List<String> learnedKeys) {
        return learnedKeys.stream().filter(content::contains).toList();
    }

    private String stableId(String lessonId, ExerciseType type, String content) {
        return UUID.nameUUIDFromBytes(
                (lessonId + "|" + type + "|" + content).getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    private void validateContent(String content, List<String> learnedKeys) {
        if (!isAllowed(content, Set.copyOf(learnedKeys))) {
            throw new IllegalStateException("Generated exercise contains an untaught key");
        }
    }

    private boolean isAllowed(String value, Set<String> learnedKeys) {
        return value.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .allMatch(character -> character.equals(" ") || learnedKeys.contains(character));
    }

    private List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private record GenerationContext(
            List<String> learnedKeys,
            List<String> weakKeys,
            List<String> prioritizedKeys,
            List<String> words) {
    }
}
