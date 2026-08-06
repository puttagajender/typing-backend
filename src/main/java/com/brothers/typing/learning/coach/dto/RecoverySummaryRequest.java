package com.brothers.typing.learning.coach.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecoverySummaryRequest(
        boolean completed,
        @NotNull(message = "keysPractised is required")
        @Size(max = 8, message = "keysPractised must not contain more than 8 keys")
        List<@NotBlank(message = "keysPractised must contain valid keys")
                @Size(max = 1, message = "each practised key must be one character") String> keysPractised,
        @DecimalMin(value = "0.0", message = "accuracyBefore must not be negative")
        @DecimalMax(value = "100.0", message = "accuracyBefore must not exceed 100") double accuracyBefore,
        @DecimalMin(value = "0.0", message = "accuracyAfter must not be negative")
        @DecimalMax(value = "100.0", message = "accuracyAfter must not exceed 100") double accuracyAfter
) { }
