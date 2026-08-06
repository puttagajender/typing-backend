package com.brothers.typing.learning.adaptive.dto;

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

public record NextStepRequest(
        @NotBlank(message = "lessonId must not be blank")
        @Size(max = 64, message = "lessonId must not exceed 64 characters") String lessonId,
        @DecimalMin(value = "0.0", message = "lessonAccuracy must not be negative")
        @DecimalMax(value = "100.0", message = "lessonAccuracy must not exceed 100") double lessonAccuracy,
        @Min(value = 1, message = "lessonDuration must be positive")
        @Max(value = 120, message = "lessonDuration must not exceed 120") int lessonDuration,
        boolean masteryAchieved,
        @NotNull(message = "weakKeys is required")
        @Size(max = 8, message = "weakKeys must not contain more than 8 keys")
        List<@NotBlank @Size(max = 1) String> weakKeys,
        @NotNull(message = "fingerPerformance is required")
        @Size(max = 8, message = "fingerPerformance must not contain more than 8 fingers")
        List<@Valid AdaptiveFingerPerformanceRequest> fingerPerformance,
        boolean recoveryCompleted,
        @DecimalMin(value = "0.0", message = "recoveryAccuracy must not be negative")
        @DecimalMax(value = "100.0", message = "recoveryAccuracy must not exceed 100") double recoveryAccuracy,
        @Min(value = 1, message = "lessonAttempts must be positive")
        @Max(value = 1000, message = "lessonAttempts must not exceed 1000") int lessonAttempts,
        @Size(max = 500, message = "aiRecommendation must not exceed 500 characters") String aiRecommendation
) {
    public NextStepRequest {
        weakKeys = weakKeys == null ? null : List.copyOf(weakKeys);
        fingerPerformance = fingerPerformance == null ? null : List.copyOf(fingerPerformance);
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Unsupported next-step request field");
    }
}
