package com.brothers.typing.learning.coach.dto;

import com.brothers.typing.learning.recovery.dto.FingerAssignment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FingerPerformanceRequest(
        @NotNull(message = "finger is required") FingerAssignment finger,
        @DecimalMin(value = "0.0", message = "finger accuracy must not be negative")
        @DecimalMax(value = "100.0", message = "finger accuracy must not exceed 100") double accuracy,
        @Min(value = 1, message = "finger attemptCount must be greater than zero") int attemptCount,
        @Min(value = 0, message = "finger mistakeCount must not be negative") int mistakeCount
) { }
