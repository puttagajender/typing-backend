package com.brothers.typing.learning.coach.dto;

import java.util.List;

public record CoachingResponse(
        String summary,
        List<String> strengths,
        List<String> focusAreas,
        int recommendedPracticeMinutes,
        List<String> recommendedKeys,
        List<String> recommendedDrills,
        boolean readyForNextLesson,
        String nextStepReason
) {
    public CoachingResponse {
        summary = summary == null ? "" : summary;
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
        recommendedKeys = recommendedKeys == null ? List.of() : List.copyOf(recommendedKeys);
        recommendedDrills = recommendedDrills == null ? List.of() : List.copyOf(recommendedDrills);
        nextStepReason = nextStepReason == null ? "" : nextStepReason;
    }
}
