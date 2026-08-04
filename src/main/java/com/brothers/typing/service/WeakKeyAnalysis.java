package com.brothers.typing.service;

import com.brothers.typing.dto.WeakKeyResponse;

import java.util.List;
import java.util.Map;

public record WeakKeyAnalysis(
        List<WeakKeyResponse> weakKeys,
        String weakKeySummary,
        Map<String, List<String>> suggestedPracticeWords
) {
}
