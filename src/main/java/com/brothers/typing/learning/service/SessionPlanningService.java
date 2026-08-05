package com.brothers.typing.learning.service;

import com.brothers.typing.learning.dto.ExerciseType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SessionPlanningService {

    private static final int EXERCISES_PER_THREE_MINUTES = 4;

    public List<ExerciseType> plan(
            ExerciseType requestedType,
            int durationMinutes,
            boolean hasWords,
            boolean hasWeakKeys) {
        int count = Math.max(1, Math.round(durationMinutes * EXERCISES_PER_THREE_MINUTES / 3.0f));
        if (requestedType != ExerciseType.MIXED) {
            return java.util.Collections.nCopies(count, requestedType);
        }

        List<ExerciseType> plan = new ArrayList<>(count);
        add(plan, ExerciseType.SINGLE_KEY, share(count, 0.15));
        add(plan, ExerciseType.MOVEMENT, share(count, 0.25));
        if (hasWords) {
            add(plan, ExerciseType.WORD, share(count, 0.20));
            add(plan, ExerciseType.PHRASE, share(count, 0.10));
        }
        add(plan, ExerciseType.RANDOM_PATTERN, share(count, 0.15));
        if (hasWeakKeys) {
            add(plan, ExerciseType.WEAK_KEY_RECOVERY, share(count, 0.15));
        }

        ExerciseType fallback = hasWords ? ExerciseType.KEY_PAIR : ExerciseType.RANDOM_PATTERN;
        while (plan.size() < count) {
            plan.add(fallback);
            fallback = fallback == ExerciseType.KEY_PAIR
                    ? ExerciseType.RANDOM_PATTERN : ExerciseType.KEY_PAIR;
        }
        return List.copyOf(plan.subList(0, count));
    }

    private int share(int count, double percentage) {
        return Math.max(1, (int) Math.round(count * percentage));
    }

    private void add(List<ExerciseType> plan, ExerciseType type, int count) {
        for (int index = 0; index < count; index++) {
            plan.add(type);
        }
    }
}
