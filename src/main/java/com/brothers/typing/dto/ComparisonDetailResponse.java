package com.brothers.typing.dto;

public record ComparisonDetailResponse(
        int originalPosition,
        int typedPosition,
        Character expectedCharacter,
        Character typedCharacter,
        MistakeType mistakeType
) {
}
