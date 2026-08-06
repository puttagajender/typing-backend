package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.dto.CoachingRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CoachingPromptTemplate {

    private static final String INSTRUCTIONS = """
            Act as a supportive typing instructor. Use only the lesson-summary JSON below.
            Focus on finger movement and accuracy. Be concise and specific, not generic.
            Recommend only keys from allowedKeys. Every drill may contain only allowedKeys and spaces.
            Do not make medical, psychological, or diagnostic claims. Never criticize harshly.
            Do not recommend progression when backendReadyForNextLesson is false.
            Return one JSON object with: summary, strengths, focusAreas,
            recommendedPracticeMinutes, recommendedKeys, recommendedDrills,
            readyForNextLesson, nextStepReason. Use at most 3 strengths, 3 focus areas,
            3 keys and 5 drills. Keep practice minutes between 3 and 10.
            """;

    private final ObjectMapper objectMapper;

    public CoachingPromptTemplate(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String create(CoachingRequest request, Set<String> allowedKeys, boolean ready) {
        PromptSummary summary = new PromptSummary(
                request.lessonId(), request.lessonName(), request.sessionDurationMinutes(),
                request.accuracy(), request.exerciseCount(), request.wordCount(),
                request.masteryAchieved(), request.weakKeys(), request.strongKeys(),
                request.fingerPerformance(), request.recoverySummary(), allowedKeys, ready);
        try {
            return INSTRUCTIONS + "\nLesson summary:\n" + objectMapper.writeValueAsString(summary);
        } catch (JacksonException exception) {
            throw new CoachingRequestException("Unable to prepare coaching summary");
        }
    }

    private record PromptSummary(
            String lessonId,
            String lessonName,
            int sessionDurationMinutes,
            double accuracy,
            int exerciseCount,
            int wordCount,
            boolean masteryAchieved,
            java.util.List<String> weakKeys,
            java.util.List<String> strongKeys,
            java.util.List<?> fingerPerformance,
            Object recoverySummary,
            Set<String> allowedKeys,
            boolean backendReadyForNextLesson
    ) { }
}
