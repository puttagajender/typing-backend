package com.brothers.typing.practice.passage;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PracticePassageCatalog {

    private final List<PracticePassage> passages;

    public PracticePassageCatalog(PracticePassageLoader loader) {
        this.passages = List.copyOf(loader.passages());
    }

    public List<PracticePassage> findAll() {
        return passages;
    }

    public List<PracticePassage> findByCategoryAndDifficulty(
            PracticeCategory category, PracticeDifficulty difficulty) {
        return passages.stream()
                .filter(passage -> passage.category() == category)
                .filter(passage -> passage.difficulty() == difficulty)
                .toList();
    }

    public long countByCategoryAndDifficulty(
            PracticeCategory category, PracticeDifficulty difficulty) {
        return passages.stream()
                .filter(passage -> passage.category() == category)
                .filter(passage -> passage.difficulty() == difficulty)
                .count();
    }
}
