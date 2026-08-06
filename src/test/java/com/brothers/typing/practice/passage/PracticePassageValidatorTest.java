package com.brothers.typing.practice.passage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticePassageValidatorTest {

    private final PracticePassageValidator validator = new PracticePassageValidator();

    @Test
    void acceptsValidPassages() {
        List<PracticePassage> passages = validator.validate(List.of(resource(
                passage("GENERAL_EASY_001", "Useful habits grow with steady practice.", 6, true))));

        assertThat(passages).hasSize(1);
    }

    @Test
    void rejectsDuplicateIdsAcrossFiles() {
        PassageResourceContent first = resource(
                passage("SAME_ID", "Steady effort supports useful progress.", 5, true));
        PassageResourceContent second = new PassageResourceContent(
                "passages/general/medium.json", PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM,
                List.of(new PracticePassage(
                        "SAME_ID", PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM,
                        "Clear plans make difficult work manageable.", 6, true)));

        assertInvalid(List.of(first, second), "duplicate id");
    }

    @Test
    void rejectsDuplicateTextAcrossFilesIgnoringCaseAndExtraSpacing() {
        PassageResourceContent first = resource(
                passage("ONE", "Steady effort supports useful progress.", 5, true));
        PassageResourceContent second = new PassageResourceContent(
                "passages/general/medium.json", PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM,
                List.of(new PracticePassage(
                        "TWO", PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM,
                        "STEADY  EFFORT SUPPORTS USEFUL PROGRESS.", 5, true)));

        assertInvalid(List.of(first, second), "duplicate text");
    }

    @Test
    void rejectsBlankText() {
        assertInvalid(List.of(resource(passage("ONE", " ", 1, true))), "text must not be blank");
    }

    @Test
    void rejectsIncorrectWordCount() {
        assertInvalid(List.of(resource(
                passage("ONE", "Practice makes movement feel natural.", 4, true))),
                "does not match calculated word count 5");
    }

    @Test
    void rejectsCategoryMismatch() {
        PracticePassage passage = new PracticePassage(
                "ONE", PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.EASY,
                "Practice makes movement feel natural.", 5, true);

        assertInvalid(List.of(resource(passage)), "category must match resource category GENERAL");
    }

    @Test
    void rejectsDifficultyMismatch() {
        PracticePassage passage = new PracticePassage(
                "ONE", PracticeCategory.GENERAL, PracticeDifficulty.HARD,
                "Practice makes movement feel natural.", 5, true);

        assertInvalid(List.of(resource(passage)), "difficulty must match resource difficulty EASY");
    }

    @Test
    void rejectsFileWithoutAnActivePassage() {
        assertInvalid(List.of(resource(
                passage("ONE", "Practice makes movement feel natural.", 5, false))),
                "file must contain at least one active passage");
    }

    @Test
    void rejectsBlankIdsNonPositiveCountsAndLineBreaks() {
        assertInvalid(List.of(resource(
                passage(" ", "Practice makes movement feel natural.", 5, true))),
                "id must not be blank");
        assertInvalid(List.of(resource(
                passage("ONE", "Practice makes movement feel natural.", 0, true))),
                "wordCount must be greater than zero");
        assertInvalid(List.of(resource(
                passage("ONE", "Practice makes\nmovement feel natural.", 5, true))),
                "text must not contain line breaks");
    }

    private PassageResourceContent resource(PracticePassage... passages) {
        return new PassageResourceContent(
                "passages/general/easy.json", PracticeCategory.GENERAL, PracticeDifficulty.EASY,
                List.of(passages));
    }

    private PracticePassage passage(
            String id, String text, int wordCount, boolean active) {
        return new PracticePassage(
                id, PracticeCategory.GENERAL, PracticeDifficulty.EASY, text, wordCount, active);
    }

    private void assertInvalid(List<PassageResourceContent> resources, String expectedMessage) {
        assertThatThrownBy(() -> validator.validate(resources))
                .isInstanceOf(PassageContentException.class)
                .hasMessageContaining(expectedMessage);
    }
}
