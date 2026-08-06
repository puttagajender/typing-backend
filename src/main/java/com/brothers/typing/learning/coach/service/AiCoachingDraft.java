package com.brothers.typing.learning.coach.service;

import java.util.List;

public record AiCoachingDraft(
        String summary,
        List<String> strengths,
        List<String> focusAreas,
        Integer recommendedPracticeMinutes,
        List<String> recommendedKeys,
        List<String> recommendedDrills,
        Boolean readyForNextLesson,
        String nextStepReason
) { }
