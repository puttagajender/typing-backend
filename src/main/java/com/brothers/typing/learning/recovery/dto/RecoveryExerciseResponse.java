package com.brothers.typing.learning.recovery.dto;

import java.util.List;

public record RecoveryExerciseResponse(
        String id,
        RecoveryExerciseType type,
        String content,
        List<String> targetKeys,
        int estimatedDurationSeconds
) {
}
