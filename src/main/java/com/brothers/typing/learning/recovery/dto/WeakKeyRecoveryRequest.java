package com.brothers.typing.learning.recovery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WeakKeyRecoveryRequest(
        @NotBlank(message = "lessonId must not be blank")
        @Size(max = 64, message = "lessonId must not exceed 64 characters") String lessonId,
        @NotEmpty(message = "learnedKeys must not be empty")
        @Size(max = 50, message = "learnedKeys must not contain more than 50 keys")
        List<@NotBlank(message = "learnedKeys must contain valid keys")
                @Size(max = 1, message = "each learned key must be one character") String> learnedKeys,
        @NotEmpty(message = "keyPerformance must not be empty")
        @Size(max = 50, message = "keyPerformance must not contain more than 50 entries")
        List<@Valid KeyPerformanceRequest> keyPerformance,
        @NotNull(message = "completedExerciseIds is required")
        @Size(max = 200, message = "completedExerciseIds must not contain more than 200 entries")
        List<@NotBlank(message = "completedExerciseIds must contain valid IDs") String> completedExerciseIds,
        @Min(value = 1, message = "requestedDurationMinutes must be at least 1")
        @Max(value = 5, message = "requestedDurationMinutes must be at most 5")
        int requestedDurationMinutes
) {
}
