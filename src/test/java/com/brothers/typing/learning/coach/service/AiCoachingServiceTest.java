package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import com.brothers.typing.learning.coach.dto.CoachingRequest;
import com.brothers.typing.learning.coach.dto.CoachingResponse;
import com.brothers.typing.learning.coach.dto.FingerPerformanceRequest;
import com.brothers.typing.learning.coach.dto.RecoverySummaryRequest;
import com.brothers.typing.learning.coach.provider.CoachingAiProvider;
import com.brothers.typing.learning.recovery.dto.FingerAssignment;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCoachingServiceTest {

    private static final String VALID_AI_JSON = """
            {
              "summary":"Your home-row control is improving.",
              "strengths":["Guide keys are stable."],
              "focusAreas":["Keep the left ring movement consistent."],
              "recommendedPracticeMinutes":5,
              "recommendedKeys":["s","l"],
              "recommendedDrills":["s s s s s","as sa ds sd"],
              "readyForNextLesson":false,
              "nextStepReason":"Repeat a short recovery session."
            }
            """;

    @Test
    void validAiResponseProducesCoaching() {
        FakeProvider provider = FakeProvider.success(VALID_AI_JSON);

        CoachingResponse response = service(enabledProperties(), provider).coach(request(false, 92.5, 15, 24));

        assertEquals("Your home-row control is improving.", response.summary());
        assertEquals(List.of("s", "l"), response.recommendedKeys());
        assertFalse(response.readyForNextLesson());
        assertEquals(1, provider.calls);
    }

    @Test
    void masteryRulesRemainAuthoritativeOverAi() {
        String aiReady = VALID_AI_JSON.replace("\"readyForNextLesson\":false", "\"readyForNextLesson\":true");

        CoachingResponse notReady = service(enabledProperties(), FakeProvider.success(aiReady))
                .coach(request(false, 99, 15, 24));
        CoachingResponse lowAccuracy = service(enabledProperties(), FakeProvider.success(aiReady))
                .coach(request(true, 94.9, 15, 24));
        CoachingResponse mastered = service(enabledProperties(), FakeProvider.success(VALID_AI_JSON))
                .coach(request(true, 97, 15, 24));

        assertFalse(notReady.readyForNextLesson());
        assertFalse(lowAccuracy.readyForNextLesson());
        assertTrue(mastered.readyForNextLesson());
    }

    @Test
    void invalidKeysAndDrillsAreRemovedAndDurationIsClamped() {
        String unsafe = VALID_AI_JSON
                .replace("\"s\",\"l\"", "\"s\",\"q\"")
                .replace("\"as sa ds sd\"", "\"quick\"")
                .replace("\"recommendedPracticeMinutes\":5", "\"recommendedPracticeMinutes\":99");

        CoachingResponse response = service(enabledProperties(), FakeProvider.success(unsafe))
                .coach(request(false, 92.5, 15, 24));

        assertEquals(List.of("s"), response.recommendedKeys());
        assertEquals(List.of("s s s s s"), response.recommendedDrills());
        assertEquals(10, response.recommendedPracticeMinutes());
    }

    @Test
    void aiDisabledMissingProviderAndMinimumThresholdUseFallbackWithoutCallingAi() {
        AiCoachingProperties disabled = enabledProperties();
        disabled.setEnabled(false);
        FakeProvider enabledProvider = FakeProvider.success(VALID_AI_JSON);
        CoachingResponse disabledResponse = service(disabled, enabledProvider)
                .coach(request(false, 92.5, 15, 24));

        FakeProvider missingProvider = FakeProvider.unavailable();
        CoachingResponse missingResponse = service(enabledProperties(), missingProvider)
                .coach(request(false, 92.5, 15, 24));

        FakeProvider belowThresholdProvider = FakeProvider.success(VALID_AI_JSON);
        CoachingResponse thresholdResponse = service(enabledProperties(), belowThresholdProvider)
                .coach(request(false, 92.5, 2, 3));

        assertEquals(0, enabledProvider.calls);
        assertEquals(0, missingProvider.calls);
        assertEquals(0, belowThresholdProvider.calls);
        assertFalse(disabledResponse.summary().isBlank());
        assertFalse(missingResponse.summary().isBlank());
        assertFalse(thresholdResponse.summary().isBlank());
    }

    @Test
    void timeoutInvalidJsonAndProviderExceptionUseFallback() {
        CoachingResponse timeout = service(enabledProperties(), FakeProvider.failure(new TimeoutException()))
                .coach(request(false, 92.5, 15, 24));
        CoachingResponse invalidJson = service(enabledProperties(), FakeProvider.success("not-json"))
                .coach(request(false, 92.5, 15, 24));
        CoachingResponse providerFailure = service(enabledProperties(), FakeProvider.failure(new RuntimeException("secret")))
                .coach(request(false, 92.5, 15, 24));

        assertFalse(timeout.summary().isBlank());
        assertFalse(invalidJson.summary().isBlank());
        assertFalse(providerFailure.summary().isBlank());
    }

    @Test
    void oneRetryAtMostIsUsedAndFallbackCanBeDisabled() {
        FakeProvider provider = FakeProvider.failure(new TimeoutException());
        service(enabledProperties(), provider).coach(request(false, 92.5, 15, 24));
        assertEquals(2, provider.calls);

        AiCoachingProperties noFallback = enabledProperties();
        noFallback.setFallbackEnabled(false);
        assertThrows(CoachingUnavailableException.class, () ->
                service(noFallback, FakeProvider.failure(new RuntimeException()))
                        .coach(request(false, 92.5, 15, 24)));
    }

    @Test
    void responseFieldsAreNeverNull() {
        CoachingResponse response = service(enabledProperties(), FakeProvider.success("{}"))
                .coach(request(false, 92.5, 15, 24));

        assertNotNull(response.summary());
        assertNotNull(response.strengths());
        assertNotNull(response.focusAreas());
        assertNotNull(response.recommendedKeys());
        assertNotNull(response.recommendedDrills());
        assertNotNull(response.nextStepReason());
    }

    @Test
    void promptContainsOnlyStructuredSummaryAndNeverRawTypingData() {
        ObjectMapper mapper = new ObjectMapper();
        CoachingPromptTemplate template = new CoachingPromptTemplate(mapper);
        String prompt = template.create(request(false, 92.5, 15, 24),
                new CoachingLessonConfiguration().learnedKeys("HOME_ROW_1"), false);

        assertTrue(prompt.contains("lessonId"));
        assertFalse(prompt.contains("typedText"));
        assertFalse(prompt.contains("keystroke"));
        assertFalse(prompt.contains("email"));
    }

    private AiCoachingService service(AiCoachingProperties properties, CoachingAiProvider provider) {
        ObjectMapper mapper = new ObjectMapper();
        return new AiCoachingService(
                properties, provider, new CoachingPromptTemplate(mapper),
                new CoachingRuleEngine(), new CoachingLessonConfiguration(),
                new FallbackCoachingService(), new CoachingResponseValidator(), mapper);
    }

    private AiCoachingProperties enabledProperties() {
        AiCoachingProperties properties = new AiCoachingProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        return properties;
    }

    private CoachingRequest request(boolean mastery, double accuracy, int duration, int exercises) {
        return new CoachingRequest(
                "HOME_ROW_1", "Home Row Foundation", duration, accuracy, exercises, 18,
                mastery, List.of("s", "l"), List.of("f", "j", "d"),
                List.of(new FingerPerformanceRequest(FingerAssignment.LEFT_RING, 84, 35, 6)),
                new RecoverySummaryRequest(true, List.of("s"), 79, 91));
    }

    private static final class FakeProvider implements CoachingAiProvider {
        private final boolean available;
        private final String content;
        private final Exception failure;
        private int calls;

        private FakeProvider(boolean available, String content, Exception failure) {
            this.available = available;
            this.content = content;
            this.failure = failure;
        }

        static FakeProvider success(String content) { return new FakeProvider(true, content, null); }
        static FakeProvider failure(Exception failure) { return new FakeProvider(true, null, failure); }
        static FakeProvider unavailable() { return new FakeProvider(false, null, null); }
        @Override public boolean isAvailable() { return available; }
        @Override public String providerName() { return "test"; }
        @Override public AiProviderResult generate(String prompt, long timeoutMillis, int maximumTokens)
                throws Exception {
            calls++;
            if (failure != null) throw failure;
            return new AiProviderResult(content, 12);
        }
    }
}
