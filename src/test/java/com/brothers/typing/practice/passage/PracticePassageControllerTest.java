package com.brothers.typing.practice.passage;

import com.brothers.typing.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PracticePassageControllerTest {

    private static final String ENDPOINT = "/api/v1/practice/passages/next";
    private MockMvc mockMvc;
    private PracticePassageCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = mock(PracticePassageCatalog.class);
        PracticePassageSelectionService service =
                new PracticePassageSelectionService(catalog, new Random(11));
        mockMvc = MockMvcBuilders.standaloneSetup(new PracticePassageController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validRequestReturnsThePublicResponseSchemaWithoutActive() throws Exception {
        when(catalog.findByCategoryAndDifficulty(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY))
                .thenReturn(List.of(passage("GENERAL_EASY_001", true)));

        mockMvc.perform(get(ENDPOINT)
                        .param("category", "GENERAL")
                        .param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("GENERAL_EASY_001"))
                .andExpect(jsonPath("$.category").value("GENERAL"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.text").value("Useful practice builds steady control."))
                .andExpect(jsonPath("$.wordCount").value(5))
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.resourcePath").doesNotExist())
                .andExpect(jsonPath("$.validation").doesNotExist());
    }

    @Test
    void missingCategoryReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("difficulty", "EASY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Required query parameter is missing: category"));
    }

    @Test
    void missingDifficultyReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("category", "GENERAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Required query parameter is missing: difficulty"));
    }

    @Test
    void invalidCategoryReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("category", "UNKNOWN")
                        .param("difficulty", "EASY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for query parameter: category"));
    }

    @Test
    void invalidDifficultyReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("category", "GENERAL")
                        .param("difficulty", "EXPERT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for query parameter: difficulty"));
    }

    @Test
    void noActivePassageReturnsNotFound() throws Exception {
        when(catalog.findByCategoryAndDifficulty(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY))
                .thenReturn(List.of(passage("INACTIVE", false)));

        mockMvc.perform(get(ENDPOINT)
                        .param("category", "GENERAL")
                        .param("difficulty", "EASY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "No active passage is available for category GENERAL and difficulty EASY."));
    }

    @Test
    void excludeIdPreventsImmediateRepetition() throws Exception {
        when(catalog.findByCategoryAndDifficulty(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY))
                .thenReturn(List.of(
                        passage("GENERAL_EASY_001", true),
                        passage("GENERAL_EASY_002", true)));

        mockMvc.perform(get(ENDPOINT)
                        .param("category", "GENERAL")
                        .param("difficulty", "EASY")
                        .param("excludeId", "GENERAL_EASY_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("GENERAL_EASY_002"));
    }

    private PracticePassage passage(String id, boolean active) {
        return new PracticePassage(
                id, PracticeCategory.GENERAL, PracticeDifficulty.EASY,
                "Useful practice builds steady control.", 5, active);
    }
}
