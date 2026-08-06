package com.brothers.typing.learning.coach.provider;

public interface CoachingAiProvider {
    boolean isAvailable();
    String providerName();
    AiProviderResult generate(String prompt, long timeoutMillis, int maximumTokens) throws Exception;

    record AiProviderResult(String content, long latencyMillis) { }
}
