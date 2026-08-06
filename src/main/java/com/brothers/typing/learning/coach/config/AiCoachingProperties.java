package com.brothers.typing.learning.coach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "typing.learning.coaching")
public class AiCoachingProperties {

    private boolean enabled = false;
    private String provider = "openai";
    private String model = "gpt-4.1-mini";
    private String apiKey = "";
    private long timeoutMillis = 8_000;
    private int maximumResponseTokens = 400;
    private int minimumSessionDurationMinutes = 5;
    private int minimumExerciseCount = 10;
    private boolean retryEnabled = true;
    private boolean fallbackEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public long getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    public int getMaximumResponseTokens() { return maximumResponseTokens; }
    public void setMaximumResponseTokens(int value) { this.maximumResponseTokens = value; }
    public int getMinimumSessionDurationMinutes() { return minimumSessionDurationMinutes; }
    public void setMinimumSessionDurationMinutes(int value) { this.minimumSessionDurationMinutes = value; }
    public int getMinimumExerciseCount() { return minimumExerciseCount; }
    public void setMinimumExerciseCount(int minimumExerciseCount) { this.minimumExerciseCount = minimumExerciseCount; }
    public boolean isRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
}
