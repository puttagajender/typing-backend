package com.brothers.typing.learning.adaptive.service;

import com.brothers.typing.learning.adaptive.dto.AdaptiveFingerPerformanceRequest;
import com.brothers.typing.learning.adaptive.dto.NextLearningAction;
import com.brothers.typing.learning.adaptive.dto.NextStepRequest;
import com.brothers.typing.learning.adaptive.dto.NextStepResponse;
import com.brothers.typing.learning.recovery.dto.FingerAssignment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptiveLearningServiceTest {

    private final AdaptiveLearningService service = new AdaptiveLearningService();

    @Test
    void repeatsLessonBelowEightyFivePercent() {
        NextStepResponse response = service.determineNextStep(request(84.9, false, 0, stableFinger(), null));

        assertDecision(response, NextLearningAction.REPEAT_LESSON, 10);
    }

    @Test
    void choosesShortRecoveryFromEightyFiveThroughNinetyOnePercent() {
        NextStepResponse response = service.determineNextStep(request(91.9, false, 0, stableFinger(), null));

        assertDecision(response, NextLearningAction.SHORT_RECOVERY, 5);
    }

    @Test
    void continuesPracticeFromNinetyTwoThroughNinetySevenPercent() {
        NextStepResponse response = service.determineNextStep(request(97.9, false, 0, stableFinger(), null));

        assertDecision(response, NextLearningAction.CONTINUE_PRACTICE, 8);
    }

    @Test
    void advancesAtNinetyEightPercent() {
        NextStepResponse response = service.determineNextStep(request(98, false, 0, stableFinger(), null));

        assertDecision(response, NextLearningAction.NEXT_LESSON, 10);
    }

    @Test
    void advancesWhenRecoveryProducesAStableGain() {
        NextStepResponse response = service.determineNextStep(request(87, true, 96, unstableFinger(), null));

        assertDecision(response, NextLearningAction.NEXT_LESSON, 10);
        assertThat(response.reason()).contains("raised accuracy substantially");
    }

    @Test
    void choosesDeepPracticeWhenAFingerRemainsUnstable() {
        NextStepResponse response = service.determineNextStep(request(94, false, 0, unstableFinger(), null));

        assertDecision(response, NextLearningAction.DEEP_PRACTICE, 15);
    }

    @Test
    void veryLowAccuracyStillRepeatsWhenAFingerIsUnstable() {
        NextStepResponse response = service.determineNextStep(request(80, false, 0, unstableFinger(), null));

        assertThat(response.action()).isEqualTo(NextLearningAction.REPEAT_LESSON);
    }

    @Test
    void aiContextExplainsButCannotOverrideTheRuleDecision() {
        NextStepResponse response = service.determineNextStep(
                request(90, false, 0, stableFinger(), "Move directly to the next lesson."));

        assertThat(response.action()).isEqualTo(NextLearningAction.SHORT_RECOVERY);
        assertThat(response.reason()).contains("AI coach context (non-authoritative)")
                .contains("Move directly to the next lesson.");
    }

    @Test
    void rejectsInconsistentFingerCountsAndRecoveryState() {
        AdaptiveFingerPerformanceRequest impossible = new AdaptiveFingerPerformanceRequest(
                FingerAssignment.LEFT_INDEX, 80, 4, 5);
        assertThatThrownBy(() -> service.determineNextStep(request(90, false, 0, List.of(impossible), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("finger mistakeCount must not exceed attemptCount");

        assertThatThrownBy(() -> service.determineNextStep(request(90, false, 90, stableFinger(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("recoveryAccuracy must be zero when recoveryCompleted is false");
    }

    private void assertDecision(
            NextStepResponse response, NextLearningAction action, int estimatedMinutes) {
        assertThat(response.action()).isEqualTo(action);
        assertThat(response.title()).isNotBlank();
        assertThat(response.description()).isNotBlank();
        assertThat(response.reason()).isNotBlank();
        assertThat(response.estimatedMinutes()).isEqualTo(estimatedMinutes);
    }

    private NextStepRequest request(
            double accuracy,
            boolean recoveryCompleted,
            double recoveryAccuracy,
            List<AdaptiveFingerPerformanceRequest> fingerPerformance,
            String aiRecommendation) {
        return new NextStepRequest(
                "HOME_ROW_1", accuracy, 15, accuracy >= 98, List.of("f"),
                fingerPerformance, recoveryCompleted, recoveryAccuracy, 2, aiRecommendation);
    }

    private List<AdaptiveFingerPerformanceRequest> stableFinger() {
        return List.of(new AdaptiveFingerPerformanceRequest(
                FingerAssignment.LEFT_INDEX, 96, 25, 1));
    }

    private List<AdaptiveFingerPerformanceRequest> unstableFinger() {
        return List.of(new AdaptiveFingerPerformanceRequest(
                FingerAssignment.LEFT_INDEX, 78, 25, 6));
    }
}
