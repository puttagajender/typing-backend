package com.brothers.typing.controller;

import com.brothers.typing.dto.TypingAnalysisResponse;
import com.brothers.typing.dto.RecommendationCategory;
import com.brothers.typing.dto.RecommendedDifficulty;
import com.brothers.typing.dto.TypingLevel;
import com.brothers.typing.service.TypingAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TypingAnalysisControllerTest {

    private static final String ENDPOINT = "/api/v1/typing/analyze";
    private static final String VALID_REQUEST = """
            {
              "originalText": "hello",
              "typedText": "hello",
              "startedAt": "2026-08-03T10:00:00Z",
              "completedAt": "2026-08-03T10:01:00Z"
            }
            """;

    private TypingAnalysisService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TypingAnalysisService.class);
        TypingAnalysisController controller = new TypingAnalysisController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validRequestReturnsHttpOkAndAnalysisResponse() throws Exception {
        TypingAnalysisResponse response = new TypingAnalysisResponse(
                1.0, 1.0, 1.0, 100.0, 60, 0, 0, 0, 0, List.of(), List.of(),
                TypingLevel.BEGINNER, "Turtle", RecommendedDifficulty.EASY,
                RecommendationCategory.GENERAL, 60, "Build consistency",
                List.of(), "No weak keys detected.", Map.of());
        when(service.analyze(any())).thenReturn(response);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.correctWpm").value(1.0))
                .andExpect(jsonPath("$.grossWpm").value(1.0))
                .andExpect(jsonPath("$.wpm").value(1.0))
                .andExpect(jsonPath("$.accuracy").value(100.0))
                .andExpect(jsonPath("$.durationInSeconds").value(60))
                .andExpect(jsonPath("$.mistakeCount").value(0))
                .andExpect(jsonPath("$.wrongCharacterCount").value(0))
                .andExpect(jsonPath("$.missingCharacterCount").value(0))
                .andExpect(jsonPath("$.extraCharacterCount").value(0))
                .andExpect(jsonPath("$.mistakeDetails").isEmpty())
                .andExpect(jsonPath("$.typingLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.typingLevelDisplayName").value("Turtle"))
                .andExpect(jsonPath("$.recommendedDifficulty").value("EASY"))
                .andExpect(jsonPath("$.recommendedCategory").value("GENERAL"))
                .andExpect(jsonPath("$.recommendedDuration").value(60))
                .andExpect(jsonPath("$.recommendationReason").value("Build consistency"))
                .andExpect(jsonPath("$.weakKeys").isEmpty())
                .andExpect(jsonPath("$.weakKeySummary").value("No weak keys detected."))
                .andExpect(jsonPath("$.suggestedPracticeWords").isMap());

        verify(service).analyze(any());
    }

    @Test
    void missingRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalText\":\"hello\""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void nullOriginalTextReturnsValidationError() throws Exception {
        assertValidationError(requestWith("null", "\"hello\"", validStart(), validEnd()),
                "originalText", "originalText must not be blank");
    }

    @Test
    void blankOriginalTextReturnsValidationError() throws Exception {
        assertValidationError(requestWith("\"   \"", "\"hello\"", validStart(), validEnd()),
                "originalText", "originalText must not be blank");
    }

    @Test
    void nullTypedTextReturnsValidationError() throws Exception {
        assertValidationError(requestWith("\"hello\"", "null", validStart(), validEnd()),
                "typedText", "typedText is required");
    }

    @Test
    void nullStartedAtReturnsValidationError() throws Exception {
        assertValidationError(requestWith("\"hello\"", "\"hello\"", "null", validEnd()),
                "startedAt", "startedAt is required");
    }

    @Test
    void nullCompletedAtReturnsValidationError() throws Exception {
        assertValidationError(requestWith("\"hello\"", "\"hello\"", validStart(), "null"),
                "completedAt", "completedAt is required");
    }

    @Test
    void completionBeforeStartReturnsBadRequestWithConsistentErrorFields() throws Exception {
        when(service.analyze(any()))
                .thenThrow(new IllegalArgumentException("completedAt must be after startedAt"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWith("\"hello\"", "\"hello\"", validEnd(), validStart())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("completedAt must be after startedAt"));
    }

    @Test
    void completionEqualToStartReturnsBadRequest() throws Exception {
        when(service.analyze(any()))
                .thenThrow(new IllegalArgumentException("completedAt must be after startedAt"));
        String sameInstant = validStart();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWith("\"hello\"", "\"hello\"", sameInstant, sameInstant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void maximumPermittedRequestSizeIsAccepted() throws Exception {
        when(service.analyze(any())).thenReturn(
                new TypingAnalysisResponse(
                        0, 0, 0, 100, 60, 0, 0, 0, 0, List.of(), List.of(),
                        TypingLevel.BEGINNER, "Turtle", RecommendedDifficulty.EASY,
                        RecommendationCategory.GENERAL, 60, "Build consistency",
                        List.of(), "No weak keys detected.", Map.of()));
        String longText = "a".repeat(6000);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithJsonStrings(longText, longText)))
                .andExpect(status().isOk());

        verify(service).analyze(any());
    }

    @Test
    void inputExceedingMaximumSizeIsRejected() throws Exception {
        String longText = "a".repeat(6001);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithJsonStrings(longText, longText)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.originalText")
                        .value("originalText must not exceed 6000 characters"))
                .andExpect(jsonPath("$.validationErrors.typedText")
                        .value("typedText must not exceed 6000 characters"));

        verifyNoInteractions(service);
    }

    @Test
    void unsupportedContentTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(service);
    }

    @Test
    void validationErrorResponseContainsConsistentFields() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWith("null", "null", "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.originalText").exists())
                .andExpect(jsonPath("$.validationErrors.typedText").exists())
                .andExpect(jsonPath("$.validationErrors.startedAt").exists())
                .andExpect(jsonPath("$.validationErrors.completedAt").exists());
    }

    @Test
    void unexpectedInternalExceptionReturnsSanitizedServerError() throws Exception {
        when(service.analyze(any())).thenThrow(new RuntimeException("database password leaked"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password leaked"))));
    }

    private void assertValidationError(String request, String field, String message) throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors." + field).value(message));

        verifyNoInteractions(service);
    }

    private String requestWith(String original, String typed, String started, String completed) {
        return """
                {"originalText":%s,"typedText":%s,"startedAt":%s,"completedAt":%s}
                """.formatted(original, typed, started, completed);
    }

    private String requestWithJsonStrings(String original, String typed) {
        return requestWith(
                "\"" + original + "\"",
                "\"" + typed + "\"",
                validStart(),
                validEnd());
    }

    private String validStart() {
        return "\"2026-08-03T10:00:00Z\"";
    }

    private String validEnd() {
        return "\"2026-08-03T10:01:00Z\"";
    }
}
