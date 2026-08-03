package com.brothers.typing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TypingAnalysisRequest(
        @NotBlank(message = "originalText must not be blank") String originalText,
        @NotNull(message = "typedText is required") String typedText,
        @NotNull(message = "startedAt is required") Instant startedAt,
        @NotNull(message = "completedAt is required") Instant completedAt
) {
}
