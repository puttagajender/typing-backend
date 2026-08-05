package com.brothers.typing.learning.dto;

import java.util.List;

public record ExerciseGenerationResponse(
        String lessonId,
        int sessionDurationMinutes,
        int estimatedExerciseCount,
        List<GeneratedExerciseResponse> exercises
) {
}
