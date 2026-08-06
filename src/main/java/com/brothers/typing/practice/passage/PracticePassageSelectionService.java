package com.brothers.typing.practice.passage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.random.RandomGenerator;

@Service
public class PracticePassageSelectionService {

    private static final Logger log = LoggerFactory.getLogger(PracticePassageSelectionService.class);

    private final PracticePassageCatalog catalog;
    private final RandomGenerator randomGenerator;

    public PracticePassageSelectionService(
            PracticePassageCatalog catalog,
            @Qualifier("practicePassageRandomGenerator") RandomGenerator randomGenerator) {
        this.catalog = catalog;
        this.randomGenerator = randomGenerator;
    }

    public PracticePassage selectNext(
            PracticeCategory category,
            PracticeDifficulty difficulty,
            String excludeId) {
        List<PracticePassage> candidates = catalog
                .findByCategoryAndDifficulty(category, difficulty).stream()
                .filter(PracticePassage::active)
                .toList();
        if (candidates.isEmpty()) {
            throw new PracticePassageNotFoundException(category, difficulty);
        }

        List<PracticePassage> alternatives = excludeId == null || excludeId.isBlank()
                ? candidates
                : candidates.stream().filter(passage -> !passage.id().equals(excludeId)).toList();
        List<PracticePassage> selectionPool = alternatives.isEmpty() ? candidates : alternatives;
        PracticePassage selected = selectionPool.get(randomGenerator.nextInt(selectionPool.size()));

        log.info("Practice passage selected: category={}, difficulty={}, excludeIdSupplied={}, passageId={}",
                category, difficulty, excludeId != null && !excludeId.isBlank(), selected.id());
        return selected;
    }
}
