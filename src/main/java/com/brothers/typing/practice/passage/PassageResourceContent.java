package com.brothers.typing.practice.passage;

import java.util.List;

public record PassageResourceContent(
        String resourcePath,
        PracticeCategory expectedCategory,
        PracticeDifficulty expectedDifficulty,
        List<PracticePassage> passages
) {
    public PassageResourceContent {
        passages = passages == null ? null : List.copyOf(passages);
    }
}
