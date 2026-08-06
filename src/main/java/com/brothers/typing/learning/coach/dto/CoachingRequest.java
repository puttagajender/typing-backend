package com.brothers.typing.learning.coach.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CoachingRequest(
        @NotBlank(message = "lessonId must not be blank")
        @Size(max = 64, message = "lessonId must not exceed 64 characters") String lessonId,
        @NotBlank(message = "lessonName must not be blank")
        @Size(max = 100, message = "lessonName must not exceed 100 characters") String lessonName,
        @Min(value = 1, message = "sessionDurationMinutes must be positive")
        @Max(value = 120, message = "sessionDurationMinutes must not exceed 120") int sessionDurationMinutes,
        @DecimalMin(value = "0.0", message = "accuracy must not be negative")
        @DecimalMax(value = "100.0", message = "accuracy must not exceed 100") double accuracy,
        @Min(value = 1, message = "exerciseCount must be positive") int exerciseCount,
        @Min(value = 0, message = "wordCount must not be negative") int wordCount,
        boolean masteryAchieved,
        @NotNull(message = "weakKeys is required") @Size(max = 8) List<@NotBlank @Size(max = 1) String> weakKeys,
        @NotNull(message = "strongKeys is required") @Size(max = 8) List<@NotBlank @Size(max = 1) String> strongKeys,
        @NotNull(message = "fingerPerformance is required") @Size(max = 8)
        List<@Valid FingerPerformanceRequest> fingerPerformance,
        @Valid RecoverySummaryRequest recoverySummary
) {
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Unsupported coaching request field");
    }
}
