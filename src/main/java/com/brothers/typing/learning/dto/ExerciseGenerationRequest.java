package com.brothers.typing.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExerciseGenerationRequest(
        @NotBlank(message = "lessonId must not be blank")
        @Size(max = 64, message = "lessonId must not exceed 64 characters")
        String lessonId,
        @NotEmpty(message = "learnedKeys must not be empty")
        @Size(max = 50, message = "learnedKeys must not contain more than 50 keys")
        List<@NotBlank(message = "learnedKeys must contain valid keys")
                @Size(max = 1, message = "each learned key must be one character") String> learnedKeys,
        @NotNull(message = "weakKeys is required")
        @Size(max = 50, message = "weakKeys must not contain more than 50 keys")
        List<@NotBlank(message = "weakKeys must contain valid keys")
                @Size(max = 1, message = "each weak key must be one character") String> weakKeys,
        @NotNull(message = "exerciseType is required") ExerciseType exerciseType,
        @NotNull(message = "difficulty is required") LearningDifficulty difficulty,
        @Min(value = 5, message = "sessionDurationMinutes must be at least 5")
        @Max(value = 30, message = "sessionDurationMinutes must be at most 30")
        int sessionDurationMinutes,
        @NotNull(message = "previousExerciseIds is required")
        @Size(max = 200, message = "previousExerciseIds must not contain more than 200 entries")
        List<@NotBlank(message = "previousExerciseIds must contain valid IDs") String> previousExerciseIds
) {
}
