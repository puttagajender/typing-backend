package com.brothers.typing.learning.recovery.dto;

import java.util.List;

public record WeakKeyRecoveryResponse(
        String lessonId,
        boolean recoveryRequired,
        List<WeakKeyResponse> weakKeys,
        String reason,
        List<RecoveryExerciseResponse> exercises
) {
}
