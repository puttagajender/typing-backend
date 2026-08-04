package com.brothers.typing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TypingAnalysisRequest(
        @NotBlank(message = "originalText must not be blank")
        @Size(max = 6000, message = "originalText must not exceed 6000 characters") String originalText,
        @NotNull(message = "typedText is required")
        @Size(max = 6000, message = "typedText must not exceed 6000 characters") String typedText,
        @NotNull(message = "startedAt is required") Instant startedAt,
        @NotNull(message = "completedAt is required") Instant completedAt
) {
}
