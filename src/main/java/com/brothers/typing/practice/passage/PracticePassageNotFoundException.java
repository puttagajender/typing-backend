package com.brothers.typing.practice.passage;

public class PracticePassageNotFoundException extends RuntimeException {

    public PracticePassageNotFoundException(
            PracticeCategory category, PracticeDifficulty difficulty) {
        super("No active passage is available for category " + category
                + " and difficulty " + difficulty + ".");
    }
}
