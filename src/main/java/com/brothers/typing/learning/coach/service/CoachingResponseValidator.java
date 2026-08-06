package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.dto.CoachingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class CoachingResponseValidator {

    public CoachingResponse validate(
            AiCoachingDraft draft,
            CoachingResponse fallback,
            Set<String> allowedKeys,
            boolean readyForNextLesson) {
        if (draft == null) return fallback;
        List<String> strengths = safeText(draft.strengths(), 3);
        List<String> focusAreas = safeText(draft.focusAreas(), 3);
        List<String> keys = safeText(draft.recommendedKeys(), 3).stream()
                .filter(allowedKeys::contains).toList();
        List<String> drills = safeText(draft.recommendedDrills(), 5).stream()
                .filter(drill -> isAllowed(drill, allowedKeys)).toList();
        int minutes = Math.max(3, Math.min(10,
                draft.recommendedPracticeMinutes() == null
                        ? fallback.recommendedPracticeMinutes()
                        : draft.recommendedPracticeMinutes()));
        String summary = safeValue(draft.summary(), fallback.summary());
        String reason = safeValue(draft.nextStepReason(), fallback.nextStepReason());
        if (!readyForNextLesson && Boolean.TRUE.equals(draft.readyForNextLesson())) {
            reason = fallback.nextStepReason();
        }
        return new CoachingResponse(
                summary,
                strengths.isEmpty() ? fallback.strengths() : strengths,
                focusAreas.isEmpty() ? fallback.focusAreas() : focusAreas,
                minutes,
                keys.isEmpty() ? fallback.recommendedKeys() : keys,
                drills.isEmpty() ? fallback.recommendedDrills() : drills,
                readyForNextLesson,
                reason);
    }

    private List<String> safeText(List<String> values, int maximum) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::trim)
                .filter(value -> !value.isBlank()).map(this::truncate).distinct()
                .limit(maximum).toList();
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : truncate(value.trim());
    }

    private String truncate(String value) {
        return value.length() <= 240 ? value : value.substring(0, 240);
    }

    private boolean isAllowed(String value, Set<String> allowedKeys) {
        return value.codePoints().mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .allMatch(character -> character.equals(" ") || allowedKeys.contains(character));
    }
}
