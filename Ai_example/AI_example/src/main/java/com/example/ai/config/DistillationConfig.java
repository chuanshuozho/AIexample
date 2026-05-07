package com.example.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.distillation")
public class DistillationConfig {

    private int tokenThreshold = 4000;
    private int preserveRecentPairs = 3;
    private int maxSummaryTokens = 500;
    private boolean enabled = true;
    private String summaryModel = "deepseek-chat";

    // Getters and Setters
    public int getTokenThreshold() {
        return tokenThreshold;
    }

    public void setTokenThreshold(int tokenThreshold) {
        this.tokenThreshold = tokenThreshold;
    }

    public int getPreserveRecentPairs() {
        return preserveRecentPairs;
    }

    public void setPreserveRecentPairs(int preserveRecentPairs) {
        this.preserveRecentPairs = preserveRecentPairs;
    }

    public int getMaxSummaryTokens() {
        return maxSummaryTokens;
    }

    public void setMaxSummaryTokens(int maxSummaryTokens) {
        this.maxSummaryTokens = maxSummaryTokens;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSummaryModel() {
        return summaryModel;
    }

    public void setSummaryModel(String summaryModel) {
        this.summaryModel = summaryModel;
    }
}
