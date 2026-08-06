package com.brothers.typing.practice.passage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PracticePassageCatalogIntegrationTest {

    @Autowired
    private PracticePassageCatalog catalog;

    @Test
    void exposesTheStartupLoadedPassagesThroughImmutableCatalogQueries() {
        assertThat(catalog.findAll()).hasSize(12);
        assertThat(catalog.findByCategoryAndDifficulty(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY))
                .hasSize(2)
                .allSatisfy(passage -> {
                    assertThat(passage.category()).isEqualTo(PracticeCategory.GENERAL);
                    assertThat(passage.difficulty()).isEqualTo(PracticeDifficulty.EASY);
                });
        assertThat(catalog.countByCategoryAndDifficulty(
                PracticeCategory.SOFTWARE_DEVELOPMENT, PracticeDifficulty.HARD)).isEqualTo(2);
        assertThatThrownBy(() -> catalog.findAll().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalog.findByCategoryAndDifficulty(
                PracticeCategory.GENERAL, PracticeDifficulty.EASY).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
