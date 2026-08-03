package com.brothers.typing.dto;

public record MistakeDetailResponse(
        int position,
        Character expectedCharacter,
        Character typedCharacter,
        MistakeType mistakeType
) {
}
