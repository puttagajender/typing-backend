package com.brothers.typing.dto;

public enum TypingLevel {
    BEGINNER("Turtle"),
    INTERMEDIATE("Rabbit"),
    ADVANCED("Horse"),
    EXPERT("Cheetah");

    private final String displayName;

    TypingLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
