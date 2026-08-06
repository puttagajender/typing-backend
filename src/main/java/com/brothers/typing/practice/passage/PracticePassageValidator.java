package com.brothers.typing.practice.passage;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PracticePassageValidator {

    public List<PracticePassage> validate(List<PassageResourceContent> resources) {
        if (resources == null || resources.isEmpty()) {
            throw new PassageContentException("No passage resources were configured");
        }

        Set<String> ids = new HashSet<>();
        Set<String> texts = new HashSet<>();
        for (PassageResourceContent resource : resources) {
            validateResource(resource, ids, texts);
        }
        return resources.stream().flatMap(resource -> resource.passages().stream()).toList();
    }

    private void validateResource(
            PassageResourceContent resource, Set<String> ids, Set<String> texts) {
        String path = resource == null ? "unknown resource" : resource.resourcePath();
        if (resource == null || resource.passages() == null || resource.passages().isEmpty()) {
            throw invalid(path, null, "file must contain at least one passage");
        }

        boolean hasActivePassage = false;
        for (PracticePassage passage : resource.passages()) {
            validatePassage(resource, passage, ids, texts);
            hasActivePassage |= passage.active();
        }
        if (!hasActivePassage) {
            throw invalid(path, null, "file must contain at least one active passage");
        }
    }

    private void validatePassage(
            PassageResourceContent resource,
            PracticePassage passage,
            Set<String> ids,
            Set<String> texts) {
        String path = resource.resourcePath();
        if (passage == null) throw invalid(path, null, "passage must not be null");
        String id = passage.id();
        if (id == null || id.isBlank()) throw invalid(path, id, "id must not be blank");
        if (!ids.add(id)) throw invalid(path, id, "duplicate id");
        if (passage.category() != resource.expectedCategory()) {
            throw invalid(path, id, "category must match resource category " + resource.expectedCategory());
        }
        if (passage.difficulty() != resource.expectedDifficulty()) {
            throw invalid(path, id, "difficulty must match resource difficulty " + resource.expectedDifficulty());
        }
        if (passage.text() == null || passage.text().isBlank()) {
            throw invalid(path, id, "text must not be blank");
        }
        if (passage.text().contains("\n") || passage.text().contains("\r")) {
            throw invalid(path, id, "text must not contain line breaks");
        }
        if (passage.wordCount() <= 0) {
            throw invalid(path, id, "wordCount must be greater than zero");
        }
        int calculatedWordCount = passage.text().trim().split("\\s+").length;
        if (passage.wordCount() != calculatedWordCount) {
            throw invalid(path, id, "wordCount " + passage.wordCount()
                    + " does not match calculated word count " + calculatedWordCount);
        }
        String normalizedText = passage.text().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (!texts.add(normalizedText)) throw invalid(path, id, "duplicate text");
    }

    private PassageContentException invalid(String path, String id, String reason) {
        String passage = id == null || id.isBlank() ? "" : ", passage=" + id;
        return new PassageContentException(
                "Invalid passage content: resource=" + path + passage + ", reason=" + reason);
    }
}
