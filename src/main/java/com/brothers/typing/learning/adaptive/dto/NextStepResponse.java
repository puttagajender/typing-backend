package com.brothers.typing.learning.adaptive.dto;

public record NextStepResponse(
        NextLearningAction action,
        String title,
        String description,
        String reason,
        int estimatedMinutes
) { }
