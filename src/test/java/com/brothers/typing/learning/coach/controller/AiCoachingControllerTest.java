package com.brothers.typing.learning.coach.controller;

import com.brothers.typing.controller.GlobalExceptionHandler;
import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import com.brothers.typing.learning.coach.provider.CoachingAiProvider;
import com.brothers.typing.learning.coach.service.AiCoachingService;
import com.brothers.typing.learning.coach.service.CoachingLessonConfiguration;
import com.brothers.typing.learning.coach.service.CoachingPromptTemplate;
import com.brothers.typing.learning.coach.service.CoachingResponseValidator;
import com.brothers.typing.learning.coach.service.CoachingRuleEngine;
import com.brothers.typing.learning.coach.service.FallbackCoachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiCoachingControllerTest {

    private static final String ENDPOINT = "/api/v1/learning/coach";
    private static final String VALID_REQUEST = """
            {
              "lessonId":"HOME_ROW_1",
              "lessonName":"Home Row Foundation",
              "sessionDurationMinutes":15,
              "accuracy":92.5,
              "exerciseCount":24,
              "wordCount":18,
              "masteryAchieved":false,
              "weakKeys":["s","l"],
              "strongKeys":["f","j","d"],
              "fingerPerformance":[{
                "finger":"LEFT_RING","accuracy":84.0,"attemptCount":35,"mistakeCount":6
              }],
              "recoverySummary":{
                "completed":true,"keysPractised":["s"],
                "accuracyBefore":79.0,"accuracyAfter":91.0
              }
            }
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiCoachingProperties properties = new AiCoachingProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        ObjectMapper mapper = new ObjectMapper();
        CoachingAiProvider failingProvider = new CoachingAiProvider() {
            @Override public boolean isAvailable() { return true; }
            @Override public String providerName() { return "test"; }
            @Override public AiProviderResult generate(String prompt, long timeout, int tokens) {
                throw new RuntimeException("provider credential and internal endpoint");
            }
        };
        AiCoachingService service = new AiCoachingService(
                properties, failingProvider, new CoachingPromptTemplate(mapper),
                new CoachingRuleEngine(), new CoachingLessonConfiguration(),
                new FallbackCoachingService(), new CoachingResponseValidator(), mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiCoachingController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validRequestReturnsUsefulFallbackContractWhenProviderFails() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.focusAreas").isArray())
                .andExpect(jsonPath("$.recommendedPracticeMinutes").value(5))
                .andExpect(jsonPath("$.recommendedKeys").isArray())
                .andExpect(jsonPath("$.recommendedDrills").isArray())
                .andExpect(jsonPath("$.readyForNextLesson").value(false))
                .andExpect(jsonPath("$.nextStepReason").isNotEmpty())
                .andExpect(content().string(not(containsString("credential"))))
                .andExpect(content().string(not(containsString("internal endpoint"))));
    }

    @Test
    void invalidRequestReturnsConsistentBadRequest() throws Exception {
        String invalid = VALID_REQUEST
                .replace("\"accuracy\":92.5", "\"accuracy\":101")
                .replace("\"exerciseCount\":24", "\"exerciseCount\":0");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.accuracy").exists())
                .andExpect(jsonPath("$.validationErrors.exerciseCount").exists());
    }

    @Test
    void semanticValidationRejectsUntaughtKeysAndImpossibleFingerCounts() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("\"s\",\"l\"", "\"s\",\"q\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coaching request contains an untaught key"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("\"mistakeCount\":6", "\"mistakeCount\":36")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("finger mistakeCount cannot exceed attemptCount"));
    }

    @Test
    void rawTypedTextIsNotAcceptedByTheCoachingContract() throws Exception {
        String requestWithRawText = VALID_REQUEST.replace(
                "\"lessonName\":\"Home Row Foundation\"",
                "\"lessonName\":\"Home Row Foundation\",\"typedText\":\"private raw text\"");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithRawText))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }
}
