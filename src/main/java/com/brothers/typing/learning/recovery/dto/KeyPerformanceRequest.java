package com.brothers.typing.learning.recovery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeyPerformanceRequest(
        @NotBlank(message = "key must not be blank")
        @Size(max = 1, message = "key must be one character") String key,
        @Min(value = 1, message = "attemptCount must be greater than zero") int attemptCount,
        @Min(value = 0, message = "mistakeCount must not be negative") int mistakeCount,
        @Min(value = 0, message = "consecutiveMistakes must not be negative") int consecutiveMistakes
) {
}
