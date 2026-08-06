package com.brothers.typing.learning.recovery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "typing.learning.recovery")
public class WeakKeyRecoveryProperties {

    private int minimumAttempts = 10;
    private double mediumMistakePercentage = 10.0;
    private double highMistakePercentage = 20.0;
    private int mediumMistakeCount = 3;
    private int consecutiveMistakeThreshold = 3;
    private int maximumWeakKeys = 3;
    private int defaultDurationMinutes = 3;
    private int maximumDurationMinutes = 5;

    public int getMinimumAttempts() { return minimumAttempts; }
    public void setMinimumAttempts(int minimumAttempts) { this.minimumAttempts = minimumAttempts; }
    public double getMediumMistakePercentage() { return mediumMistakePercentage; }
    public void setMediumMistakePercentage(double value) { this.mediumMistakePercentage = value; }
    public double getHighMistakePercentage() { return highMistakePercentage; }
    public void setHighMistakePercentage(double value) { this.highMistakePercentage = value; }
    public int getMediumMistakeCount() { return mediumMistakeCount; }
    public void setMediumMistakeCount(int mediumMistakeCount) { this.mediumMistakeCount = mediumMistakeCount; }
    public int getConsecutiveMistakeThreshold() { return consecutiveMistakeThreshold; }
    public void setConsecutiveMistakeThreshold(int value) { this.consecutiveMistakeThreshold = value; }
    public int getMaximumWeakKeys() { return maximumWeakKeys; }
    public void setMaximumWeakKeys(int maximumWeakKeys) { this.maximumWeakKeys = maximumWeakKeys; }
    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(int value) { this.defaultDurationMinutes = value; }
    public int getMaximumDurationMinutes() { return maximumDurationMinutes; }
    public void setMaximumDurationMinutes(int value) { this.maximumDurationMinutes = value; }
}
