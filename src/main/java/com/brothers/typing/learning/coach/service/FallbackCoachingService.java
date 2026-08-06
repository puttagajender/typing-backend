package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.dto.CoachingRequest;
import com.brothers.typing.learning.coach.dto.CoachingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FallbackCoachingService {

    public CoachingResponse create(
            CoachingRequest request, Set<String> allowedKeys, boolean readyForNextLesson) {
        List<String> strengths = new ArrayList<>();
        if (!request.strongKeys().isEmpty()) {
            strengths.add("Control of " + String.join(", ", request.strongKeys()) + " is becoming stable.");
        }
        if (request.recoverySummary() != null && request.recoverySummary().completed()
                && request.recoverySummary().accuracyAfter() > request.recoverySummary().accuracyBefore()) {
            strengths.add("Focused-practice accuracy improved during this session.");
        }
        if (strengths.isEmpty()) strengths.add("You completed a useful accuracy-focused session.");

        List<String> recommendedKeys = request.weakKeys().stream()
                .filter(allowedKeys::contains).distinct().limit(3).toList();
        List<String> focusAreas = recommendedKeys.isEmpty()
                ? List.of("Keep movements controlled while building consistent rhythm.")
                : List.of("Practice " + String.join(", ", recommendedKeys)
                        + " with slow and consistent finger movement.");
        List<String> drills = fallbackDrills(recommendedKeys, allowedKeys);
        String summary = readyForNextLesson
                ? "Your lesson control meets the current mastery requirements."
                : "Your key control is improving with focused practice.";
        String reason = readyForNextLesson
                ? "The mastery result and accuracy support moving to the next lesson."
                : "Repeat a short focused session before moving forward.";
        return new CoachingResponse(
                summary, strengths.stream().limit(3).toList(), focusAreas, 5,
                recommendedKeys, drills, readyForNextLesson, reason);
    }

    private List<String> fallbackDrills(List<String> weakKeys, Set<String> allowedKeys) {
        String target = weakKeys.isEmpty() ? allowedKeys.stream().sorted().findFirst().orElse("a") : weakKeys.get(0);
        List<String> others = allowedKeys.stream().filter(key -> !key.equals(target)).sorted().limit(2).toList();
        LinkedHashSet<String> drills = new LinkedHashSet<>();
        drills.add(String.join(" ", java.util.Collections.nCopies(5, target)));
        for (String other : others) drills.add(other + target + " " + target + other);
        return List.copyOf(drills);
    }
}
