package com.brothers.typing.learning.recovery.service;

import com.brothers.typing.learning.recovery.dto.FingerAssignment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class HomeRowFingerMap {

    private static final List<String> ORDERED_KEYS = List.of("a", "s", "d", "f", "j", "k", "l", ";");
    private static final Map<String, FingerAssignment> FINGERS = Map.of(
            "a", FingerAssignment.LEFT_PINKY,
            "s", FingerAssignment.LEFT_RING,
            "d", FingerAssignment.LEFT_MIDDLE,
            "f", FingerAssignment.LEFT_INDEX,
            "j", FingerAssignment.RIGHT_INDEX,
            "k", FingerAssignment.RIGHT_MIDDLE,
            "l", FingerAssignment.RIGHT_RING,
            ";", FingerAssignment.RIGHT_PINKY);

    public boolean supportsLesson(String lessonId) {
        return "HOME_ROW_1".equals(lessonId);
    }

    public Set<String> supportedKeys() {
        return FINGERS.keySet();
    }

    public FingerAssignment fingerFor(String key) {
        return FINGERS.get(key);
    }

    public List<String> neighbours(String key, Set<String> learnedKeys) {
        int position = ORDERED_KEYS.indexOf(key);
        if (position < 0) return List.of();
        java.util.ArrayList<String> neighbours = new java.util.ArrayList<>(2);
        if (position > 0 && learnedKeys.contains(ORDERED_KEYS.get(position - 1))) {
            neighbours.add(ORDERED_KEYS.get(position - 1));
        }
        if (position + 1 < ORDERED_KEYS.size()
                && learnedKeys.contains(ORDERED_KEYS.get(position + 1))) {
            neighbours.add(ORDERED_KEYS.get(position + 1));
        }
        return List.copyOf(neighbours);
    }
}
