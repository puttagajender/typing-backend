package com.brothers.typing.learning.dto;

import java.util.List;

public record GeneratedExerciseResponse(
        String id,
        ExerciseType type,
        String content,
        List<String> targetKeys,
        int estimatedDurationSeconds
) {
}
