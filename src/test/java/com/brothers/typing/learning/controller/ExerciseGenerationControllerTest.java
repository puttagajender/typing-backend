package com.brothers.typing.learning.controller;

import com.brothers.typing.controller.GlobalExceptionHandler;
import com.brothers.typing.learning.service.ExerciseGenerationService;
import com.brothers.typing.learning.service.LessonWordDictionary;
import com.brothers.typing.learning.service.SessionPlanningService;
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

class ExerciseGenerationControllerTest {

    private static final String ENDPOINT = "/api/v1/learning/exercises/generate";
    private static final String VALID_REQUEST = """
            {
              "lessonId": "HOME_ROW_1",
              "learnedKeys": ["a", "s", "d", "f", "j", "k", "l", ";"],
              "weakKeys": ["s"],
              "exerciseType": "MIXED",
              "difficulty": "BEGINNER",
              "sessionDurationMinutes": 15,
              "previousExerciseIds": []
            }
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExerciseGenerationService service = new ExerciseGenerationService(
                new LessonWordDictionary(), new SessionPlanningService());
        mockMvc = MockMvcBuilders.standaloneSetup(new ExerciseGenerationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validRequestReturnsStructuredSafeSession() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value("HOME_ROW_1"))
                .andExpect(jsonPath("$.sessionDurationMinutes").value(15))
                .andExpect(jsonPath("$.estimatedExerciseCount").value(20))
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.exercises[0].id").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].type").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].content").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].targetKeys").isArray())
                .andExpect(jsonPath("$.exercises[0].estimatedDurationSeconds").isNumber())
                .andExpect(jsonPath("$.exercises[*].content",
                        everyItem(matchesPattern("[asdfjkl; ]+"))));
    }

    @Test
    void beanValidationFailuresReturnConsistentBadRequest() throws Exception {
        String invalid = VALID_REQUEST
                .replace("\"HOME_ROW_1\"", "\" \"")
                .replace("15,", "4,");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.lessonId").exists())
                .andExpect(jsonPath("$.validationErrors.sessionDurationMinutes").exists());
    }

    @Test
    void weakKeyOutsideLearnedKeysReturnsBadRequest() throws Exception {
        String invalid = VALID_REQUEST.replace("\"weakKeys\": [\"s\"]", "\"weakKeys\": [\"q\"]");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Every weak key must exist in learnedKeys"));
    }

    @Test
    void unsupportedExerciseTypeReturnsSanitizedBadRequest() throws Exception {
        String invalid = VALID_REQUEST.replace("\"MIXED\"", "\"NOT_SUPPORTED\"");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request body is missing or malformed"));
    }

    @Test
    void unsupportedLessonReturnsBadRequest() throws Exception {
        String invalid = VALID_REQUEST.replace("HOME_ROW_1", "UNKNOWN_LESSON");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported lesson"));
    }
}
