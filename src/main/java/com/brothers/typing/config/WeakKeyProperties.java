package com.brothers.typing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "typing.weak-key")
public class WeakKeyProperties {

    private int minimumSpaceMistakes = 3;
    private int maximumWeakKeys = 5;
    private String noWeakKeysSummary = "No weak keys detected.";
    private Map<String, List<String>> practiceWords = new LinkedHashMap<>();

    public int getMinimumSpaceMistakes() {
        return minimumSpaceMistakes;
    }

    public void setMinimumSpaceMistakes(int minimumSpaceMistakes) {
        this.minimumSpaceMistakes = minimumSpaceMistakes;
    }

    public int getMaximumWeakKeys() {
        return maximumWeakKeys;
    }

    public void setMaximumWeakKeys(int maximumWeakKeys) {
        this.maximumWeakKeys = maximumWeakKeys;
    }

    public String getNoWeakKeysSummary() {
        return noWeakKeysSummary;
    }

    public void setNoWeakKeysSummary(String noWeakKeysSummary) {
        this.noWeakKeysSummary = noWeakKeysSummary;
    }

    public Map<String, List<String>> getPracticeWords() {
        return practiceWords;
    }

    public void setPracticeWords(Map<String, List<String>> practiceWords) {
        this.practiceWords = practiceWords;
    }
}
