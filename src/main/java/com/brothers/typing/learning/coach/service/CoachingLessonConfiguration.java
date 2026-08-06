package com.brothers.typing.learning.coach.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CoachingLessonConfiguration {

    private static final Set<String> HOME_ROW_KEYS = Set.of("a", "s", "d", "f", "j", "k", "l", ";");

    public Set<String> learnedKeys(String lessonId) {
        if (!"HOME_ROW_1".equals(lessonId)) {
            throw new CoachingRequestException("Unsupported lesson");
        }
        return HOME_ROW_KEYS;
    }
}
