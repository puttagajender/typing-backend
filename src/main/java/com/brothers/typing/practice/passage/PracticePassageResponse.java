package com.brothers.typing.practice.passage;

public record PracticePassageResponse(
        String id,
        PracticeCategory category,
        PracticeDifficulty difficulty,
        String text,
        int wordCount
) {
    public static PracticePassageResponse from(PracticePassage passage) {
        return new PracticePassageResponse(
                passage.id(), passage.category(), passage.difficulty(),
                passage.text(), passage.wordCount());
    }
}
