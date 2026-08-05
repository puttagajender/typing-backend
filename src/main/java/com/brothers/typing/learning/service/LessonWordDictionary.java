package com.brothers.typing.learning.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Local, reviewed vocabulary grouped by lesson capability. */
@Component
public class LessonWordDictionary {

    private static final Map<String, List<String>> WORDS_BY_LESSON = Map.of(
            "HOME_ROW_1", List.of(
                    "a", "as", "ask", "dad", "fall", "flask", "sad", "salad", "all", "lass")
    );

    public boolean supports(String lessonId) {
        return WORDS_BY_LESSON.containsKey(lessonId);
    }

    public List<String> wordsFor(String lessonId) {
        return WORDS_BY_LESSON.getOrDefault(lessonId, List.of());
    }
}
