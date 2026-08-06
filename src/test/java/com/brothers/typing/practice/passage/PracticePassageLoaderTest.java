package com.brothers.typing.practice.passage;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticePassageLoaderTest {

    @Test
    void loadsAllSixFilesFromTheClasspathAndValidatesEverySample() {
        PracticePassageLoader loader = new PracticePassageLoader(
                new ObjectMapper(), new DefaultResourceLoader(), new PracticePassageValidator());

        assertThat(loader.passages())
                .hasSize(12)
                .allSatisfy(passage -> {
                    assertThat(passage.id()).isNotBlank();
                    assertThat(passage.text()).isNotBlank();
                    assertThat(passage.wordCount()).isPositive();
                    assertThat(passage.active()).isTrue();
                });
        for (PracticeCategory category : PracticeCategory.values()) {
            for (PracticeDifficulty difficulty : PracticeDifficulty.values()) {
                assertThat(loader.passages())
                        .filteredOn(passage -> passage.category() == category)
                        .filteredOn(passage -> passage.difficulty() == difficulty)
                        .hasSize(2);
            }
        }
        assertThatThrownBy(() -> loader.passages().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
