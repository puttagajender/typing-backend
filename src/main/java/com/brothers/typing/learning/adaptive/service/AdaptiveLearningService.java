package com.brothers.typing.learning.adaptive.service;

import com.brothers.typing.learning.adaptive.dto.AdaptiveFingerPerformanceRequest;
import com.brothers.typing.learning.adaptive.dto.NextLearningAction;
import com.brothers.typing.learning.adaptive.dto.NextStepRequest;
import com.brothers.typing.learning.adaptive.dto.NextStepResponse;
import com.brothers.typing.learning.recovery.dto.FingerAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AdaptiveLearningService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveLearningService.class);

    static final double REPEAT_LESSON_MAX_ACCURACY = 85.0;
    static final double SHORT_RECOVERY_MAX_ACCURACY = 92.0;
    static final double NEXT_LESSON_MIN_ACCURACY = 98.0;
    static final double GREAT_RECOVERY_MIN_ACCURACY = 95.0;
    static final double GREAT_RECOVERY_MIN_GAIN = 8.0;
    static final double UNSTABLE_FINGER_MAX_ACCURACY = 85.0;
    static final int UNSTABLE_FINGER_MIN_ATTEMPTS = 10;

    public NextStepResponse determineNextStep(NextStepRequest request) {
        validateConsistency(request);

        Decision decision;
        if (request.lessonAccuracy() < REPEAT_LESSON_MAX_ACCURACY) {
            decision = new Decision(
                    NextLearningAction.REPEAT_LESSON,
                    "Repeat This Lesson",
                    "Rebuild the lesson slowly with accuracy as the only goal.",
                    "Accuracy is below 85%, so repeating the same movements is safer than adding complexity.",
                    10);
        } else if (recoveryImprovedGreatly(request)) {
            decision = new Decision(
                    NextLearningAction.NEXT_LESSON,
                    "Continue to the Next Lesson",
                    "Carry the improved control into the next set of keys.",
                    "Focused recovery raised accuracy substantially and finished at a stable level.",
                    10);
        } else if (hasUnstableFinger(request)) {
            decision = new Decision(
                    NextLearningAction.DEEP_PRACTICE,
                    "Strengthen Finger Control",
                    "Use a longer focused session for the finger that is still inconsistent.",
                    "At least one finger remains unstable after enough attempts to make the pattern meaningful.",
                    15);
        } else if (request.lessonAccuracy() >= NEXT_LESSON_MIN_ACCURACY) {
            decision = new Decision(
                    NextLearningAction.NEXT_LESSON,
                    "Continue to the Next Lesson",
                    "Build on this accurate performance with the next lesson.",
                    "Accuracy is at least 98%, demonstrating stable control of the current lesson.",
                    10);
        } else if (request.lessonAccuracy() >= SHORT_RECOVERY_MAX_ACCURACY) {
            decision = new Decision(
                    NextLearningAction.CONTINUE_PRACTICE,
                    "Continue Practising",
                    "Complete another short mixed set before advancing.",
                    "Accuracy is between 92% and 97%, so a little more repetition should improve consistency.",
                    8);
        } else {
            decision = new Decision(
                    NextLearningAction.SHORT_RECOVERY,
                    "Take a Short Recovery",
                    "Focus briefly on the weak keys before returning to the lesson.",
                    "Accuracy is between 85% and 91%, indicating a small number of movements need repair.",
                    5);
        }

        String reason = contextualReason(decision.reason(), request);
        NextStepResponse response = new NextStepResponse(
                decision.action(), decision.title(), decision.description(), reason, decision.estimatedMinutes());
        log.info("Adaptive next step determined: lessonId={}, action={}, accuracy={}, attempt={}",
                request.lessonId(), response.action(), request.lessonAccuracy(), request.lessonAttempts());
        return response;
    }

    private boolean recoveryImprovedGreatly(NextStepRequest request) {
        return request.recoveryCompleted()
                && request.recoveryAccuracy() >= GREAT_RECOVERY_MIN_ACCURACY
                && request.recoveryAccuracy() - request.lessonAccuracy() >= GREAT_RECOVERY_MIN_GAIN;
    }

    private boolean hasUnstableFinger(NextStepRequest request) {
        return request.fingerPerformance().stream().anyMatch(performance ->
                performance.attemptCount() >= UNSTABLE_FINGER_MIN_ATTEMPTS
                        && performance.accuracy() < UNSTABLE_FINGER_MAX_ACCURACY);
    }

    private void validateConsistency(NextStepRequest request) {
        Set<FingerAssignment> fingers = new HashSet<>();
        for (AdaptiveFingerPerformanceRequest performance : request.fingerPerformance()) {
            if (performance.mistakeCount() > performance.attemptCount()) {
                throw new IllegalArgumentException("finger mistakeCount must not exceed attemptCount");
            }
            if (!fingers.add(performance.finger())) {
                throw new IllegalArgumentException("fingerPerformance must not contain duplicate fingers");
            }
        }
        if (!request.recoveryCompleted() && request.recoveryAccuracy() > 0) {
            throw new IllegalArgumentException(
                    "recoveryAccuracy must be zero when recoveryCompleted is false");
        }
    }

    private String contextualReason(String ruleReason, NextStepRequest request) {
        StringBuilder reason = new StringBuilder(ruleReason);
        if (!request.masteryAchieved()) {
            reason.append(" The current lesson is not yet marked as mastered.");
        }
        if (request.lessonAttempts() > 1) {
            reason.append(" This decision considers attempt ").append(request.lessonAttempts()).append('.');
        }
        if (request.aiRecommendation() != null && !request.aiRecommendation().isBlank()) {
            reason.append(" AI coach context (non-authoritative): ")
                    .append(request.aiRecommendation().trim());
        }
        return reason.toString();
    }

    private record Decision(
            NextLearningAction action,
            String title,
            String description,
            String reason,
            int estimatedMinutes
    ) { }
}
