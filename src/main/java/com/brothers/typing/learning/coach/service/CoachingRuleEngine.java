package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.dto.CoachingRequest;
import org.springframework.stereotype.Component;

@Component
public class CoachingRuleEngine {

    static final double MINIMUM_MASTERY_ACCURACY = 95.0;

    public boolean readyForNextLesson(CoachingRequest request) {
        return request.masteryAchieved() && request.accuracy() >= MINIMUM_MASTERY_ACCURACY;
    }
}
