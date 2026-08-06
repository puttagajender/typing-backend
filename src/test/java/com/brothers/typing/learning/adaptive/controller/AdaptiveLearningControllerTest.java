package com.brothers.typing.learning.adaptive.controller;

import com.brothers.typing.controller.GlobalExceptionHandler;
import com.brothers.typing.learning.adaptive.service.AdaptiveLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdaptiveLearningControllerTest {

    private static final String ENDPOINT = "/api/v1/learning/next-step";
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdaptiveLearningController(new AdaptiveLearningService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheAdaptiveDecisionContract() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(98)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("NEXT_LESSON"))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.estimatedMinutes").value(10));
    }

    @Test
    void validatesRequiredFieldsAndRanges() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": "",
                                  "lessonAccuracy": 101,
                                  "lessonDuration": 0,
                                  "masteryAchieved": false,
                                  "weakKeys": null,
                                  "fingerPerformance": [],
                                  "recoveryCompleted": false,
                                  "recoveryAccuracy": 0,
                                  "lessonAttempts": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.lessonId").exists())
                .andExpect(jsonPath("$.validationErrors.lessonAccuracy").exists())
                .andExpect(jsonPath("$.validationErrors.lessonDuration").exists())
                .andExpect(jsonPath("$.validationErrors.weakKeys").exists())
                .andExpect(jsonPath("$.validationErrors.lessonAttempts").exists());
    }

    @Test
    void rejectsUnknownRequestFields() throws Exception {
        String request = validRequest(94).replace("\n}", ",\n  \"rawTyping\": \"secret\"\n}");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    private String validRequest(double accuracy) {
        return """
                {
                  "lessonId": "HOME_ROW_1",
                  "lessonAccuracy": %s,
                  "lessonDuration": 15,
                  "masteryAchieved": true,
                  "weakKeys": ["f"],
                  "fingerPerformance": [
                    {"finger": "LEFT_INDEX", "accuracy": 96, "attemptCount": 25, "mistakeCount": 1}
                  ],
                  "recoveryCompleted": false,
                  "recoveryAccuracy": 0,
                  "lessonAttempts": 1,
                  "aiRecommendation": "Maintain relaxed movement."
                }
                """.formatted(accuracy);
    }
}
