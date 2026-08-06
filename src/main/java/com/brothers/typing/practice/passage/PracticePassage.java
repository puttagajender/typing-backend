package com.brothers.typing.practice.passage;

public record PracticePassage(
        String id,
        PracticeCategory category,
        PracticeDifficulty difficulty,
        String text,
        int wordCount,
        boolean active
) { }
