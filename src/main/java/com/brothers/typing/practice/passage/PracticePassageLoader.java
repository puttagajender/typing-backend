package com.brothers.typing.practice.passage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PracticePassageLoader {

    private static final Logger log = LoggerFactory.getLogger(PracticePassageLoader.class);
    private static final List<ResourceDefinition> RESOURCE_DEFINITIONS = List.of(
            definition("passages/general/easy.json", PracticeCategory.GENERAL, PracticeDifficulty.EASY),
            definition("passages/general/medium.json", PracticeCategory.GENERAL, PracticeDifficulty.MEDIUM),
            definition("passages/general/hard.json", PracticeCategory.GENERAL, PracticeDifficulty.HARD),
            definition("passages/software-development/easy.json", PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.EASY),
            definition("passages/software-development/medium.json", PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.MEDIUM),
            definition("passages/software-development/hard.json", PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.HARD));

    private final List<PracticePassage> passages;

    public PracticePassageLoader(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            PracticePassageValidator validator) {
        this.passages = load(objectMapper, resourceLoader, validator);
        logSummary(passages);
    }

    public List<PracticePassage> passages() {
        return passages;
    }

    private List<PracticePassage> load(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            PracticePassageValidator validator) {
        List<PassageResourceContent> contents = new ArrayList<>();
        for (ResourceDefinition definition : RESOURCE_DEFINITIONS) {
            contents.add(readResource(objectMapper, resourceLoader, definition));
        }
        try {
            return List.copyOf(validator.validate(contents));
        } catch (PassageContentException exception) {
            log.error("Passage content validation failed: {}", exception.getMessage());
            throw exception;
        }
    }

    private PassageResourceContent readResource(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            ResourceDefinition definition) {
        Resource resource = resourceLoader.getResource("classpath:" + definition.path());
        if (!resource.exists()) {
            PassageContentException exception = new PassageContentException(
                    "Passage resource is missing: " + definition.path());
            log.error(exception.getMessage());
            throw exception;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            PracticePassage[] values = objectMapper.readValue(inputStream, PracticePassage[].class);
            return new PassageResourceContent(
                    definition.path(), definition.category(), definition.difficulty(),
                    values == null ? List.of() : Arrays.asList(values));
        } catch (Exception exception) {
            String message = "Unable to load passage resource: " + definition.path();
            log.error(message, exception);
            throw new PassageContentException(message, exception);
        }
    }

    private void logSummary(List<PracticePassage> loadedPassages) {
        long activeCount = loadedPassages.stream().filter(PracticePassage::active).count();
        log.info("Total passages loaded: {}", loadedPassages.size());
        log.info("Active passage count: {}", activeCount);
        for (PracticeCategory category : PracticeCategory.values()) {
            for (PracticeDifficulty difficulty : PracticeDifficulty.values()) {
                long count = loadedPassages.stream()
                        .filter(passage -> passage.category() == category)
                        .filter(passage -> passage.difficulty() == difficulty)
                        .count();
                log.info("Passage count: category={}, difficulty={}, count={}",
                        category, difficulty, count);
            }
        }
    }

    private static ResourceDefinition definition(
            String path, PracticeCategory category, PracticeDifficulty difficulty) {
        return new ResourceDefinition(path, category, difficulty);
    }

    private record ResourceDefinition(
            String path, PracticeCategory category, PracticeDifficulty difficulty) { }
}
