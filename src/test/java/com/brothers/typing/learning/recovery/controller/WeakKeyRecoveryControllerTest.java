package com.brothers.typing.learning.recovery.controller;

import com.brothers.typing.controller.GlobalExceptionHandler;
import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.recovery.service.HomeRowFingerMap;
import com.brothers.typing.learning.recovery.service.WeakKeyAnalysisService;
import com.brothers.typing.learning.recovery.service.WeakKeyRecoveryService;
import com.brothers.typing.learning.service.LessonWordDictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeakKeyRecoveryControllerTest {

    private static final String ENDPOINT = "/api/v1/learning/recovery/generate";
    private static final String VALID_REQUEST = """
            {
              "lessonId": "HOME_ROW_1",
              "learnedKeys": ["a", "s", "d", "f", "j", "k", "l", ";"],
              "keyPerformance": [
                {"key": "s", "attemptCount": 25, "mistakeCount": 6, "consecutiveMistakes": 2},
                {"key": "d", "attemptCount": 20, "mistakeCount": 1, "consecutiveMistakes": 0}
              ],
              "completedExerciseIds": [],
              "requestedDurationMinutes": 3
            }
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WeakKeyRecoveryProperties properties = new WeakKeyRecoveryProperties();
        WeakKeyRecoveryService service = new WeakKeyRecoveryService(
                new WeakKeyAnalysisService(properties), properties,
                new HomeRowFingerMap(), new LessonWordDictionary());
        mockMvc = MockMvcBuilders.standaloneSetup(new WeakKeyRecoveryController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validRequestReturnsRecoveryContractWithSafeContent() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value("HOME_ROW_1"))
                .andExpect(jsonPath("$.recoveryRequired").value(true))
                .andExpect(jsonPath("$.weakKeys[0].key").value("s"))
                .andExpect(jsonPath("$.weakKeys[0].mistakeCount").value(6))
                .andExpect(jsonPath("$.weakKeys[0].mistakePercentage").value(24.0))
                .andExpect(jsonPath("$.weakKeys[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].id").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].type").value("SINGLE_KEY"))
                .andExpect(jsonPath("$.exercises[0].targetKeys[0]").value("s"))
                .andExpect(jsonPath("$.exercises[0].estimatedDurationSeconds").isNumber())
                .andExpect(jsonPath("$.exercises[*].content",
                        everyItem(matchesPattern("[asdfjkl; ]+"))));
    }

    @Test
    void noRecoveryReturnsSuccessfulEmptyPlan() throws Exception {
        String noRecovery = VALID_REQUEST
                .replace("\"mistakeCount\": 6", "\"mistakeCount\": 0")
                .replace("\"consecutiveMistakes\": 2", "\"consecutiveMistakes\": 0")
                .replace("\"mistakeCount\": 1", "\"mistakeCount\": 0");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noRecovery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRequired").value(false))
                .andExpect(jsonPath("$.weakKeys").isEmpty())
                .andExpect(jsonPath("$.exercises").isEmpty())
                .andExpect(jsonPath("$.reason")
                        .value("No focused recovery is needed. Continue normal practice."));
    }

    @Test
    void beanValidationRejectsInvalidCountsAndDuration() throws Exception {
        String invalid = VALID_REQUEST
                .replace("\"attemptCount\": 25", "\"attemptCount\": 0")
                .replace("\"requestedDurationMinutes\": 3", "\"requestedDurationMinutes\": 6");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.requestedDurationMinutes").exists());
    }

    @Test
    void semanticValidationRejectsMistakesAboveAttemptsAndUnknownKey() throws Exception {
        String excessiveMistakes = VALID_REQUEST.replace(
                "\"mistakeCount\": 6", "\"mistakeCount\": 26");
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(excessiveMistakes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("mistakeCount cannot exceed attemptCount"));

        String unknownKey = VALID_REQUEST.replace("\"key\": \"s\"", "\"key\": \"q\"");
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Every performance key must exist in learnedKeys"));
    }
}
