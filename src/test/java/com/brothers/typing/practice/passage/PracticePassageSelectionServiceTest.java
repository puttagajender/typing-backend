package com.brothers.typing.practice.passage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticePassageSelectionServiceTest {

    @ParameterizedTest
    @MethodSource("categoryAndDifficultyCombinations")
    void returnsAMatchingPassageForEverySupportedCombination(
            PracticeCategory category, PracticeDifficulty difficulty) {
        PracticePassage expected = passage("MATCH", category, difficulty, true);
        PracticePassageSelectionService service = service(List.of(expected), 1);

        assertThat(service.selectNext(category, difficulty, null)).isEqualTo(expected);
    }

    @Test
    void selectsOnlyActivePassages() {
        PracticePassage active = passage(
                "ACTIVE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true);
        PracticePassage inactive = passage(
                "INACTIVE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, false);
        PracticePassageSelectionService service = service(List.of(inactive, active), 3);

        assertThat(service.selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, null)).isEqualTo(active);
    }

    @Test
    void excludesThePreviousPassageWhenAnAlternativeExists() {
        PracticePassage previous = passage(
                "PREVIOUS", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true);
        PracticePassage alternative = passage(
                "ALTERNATIVE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true);
        PracticePassageSelectionService service = service(List.of(previous, alternative), 4);

        assertThat(service.selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, "PREVIOUS"))
                .isEqualTo(alternative);
    }

    @Test
    void fallsBackToTheExcludedPassageWhenItIsTheOnlyMatch() {
        PracticePassage only = passage(
                "ONLY", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true);
        PracticePassageSelectionService service = service(List.of(only), 5);

        assertThat(service.selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, "ONLY")).isEqualTo(only);
    }

    @Test
    void ignoresAnUnknownExcludedId() {
        List<PracticePassage> passages = List.of(
                passage("ONE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true),
                passage("TWO", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true));
        PracticePassageSelectionService service = service(passages, 6);

        assertThat(service.selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, "UNKNOWN"))
                .isIn(passages);
    }

    @Test
    void throwsNotFoundWhenNoActivePassageMatches() {
        PracticePassageSelectionService service = service(List.of(
                passage("INACTIVE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, false)), 7);

        assertThatThrownBy(() -> service.selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, null))
                .isInstanceOf(PracticePassageNotFoundException.class)
                .hasMessage("No active passage is available for category GENERAL and difficulty EASY.");
    }

    @Test
    void seededSelectionIsDeterministic() {
        List<PracticePassage> passages = List.of(
                passage("ONE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true),
                passage("TWO", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true),
                passage("THREE", PracticeCategory.GENERAL, PracticeDifficulty.EASY, true));

        PracticePassage first = service(passages, 42).selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, null);
        PracticePassage second = service(passages, 42).selectNext(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY, null);

        assertThat(first).isEqualTo(second);
    }

    private PracticePassageSelectionService service(
            List<PracticePassage> passages, long seed) {
        PracticePassageCatalog catalog = mock(PracticePassageCatalog.class);
        PracticeCategory category = passages.get(0).category();
        PracticeDifficulty difficulty = passages.get(0).difficulty();
        when(catalog.findByCategoryAndDifficulty(category, difficulty)).thenReturn(passages);
        return new PracticePassageSelectionService(catalog, new Random(seed));
    }

    private PracticePassage passage(
            String id,
            PracticeCategory category,
            PracticeDifficulty difficulty,
            boolean active) {
        return new PracticePassage(
                id, category, difficulty, "Useful practice builds steady control.", 5, active);
    }

    private static Stream<Arguments> categoryAndDifficultyCombinations() {
        return Stream.of(
                Arguments.of(PracticeCategory.GENERAL, PracticeDifficulty.EASY),
                Arguments.of(PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM),
                Arguments.of(PracticeCategory.GENERAL, PracticeDifficulty.HARD),
                Arguments.of(PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.EASY),
                Arguments.of(PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.MEDIUM),
                Arguments.of(PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.HARD));
    }
}
