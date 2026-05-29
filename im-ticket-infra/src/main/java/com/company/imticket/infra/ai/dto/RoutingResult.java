package com.company.imticket.infra.ai.dto;

import java.util.Map;

public class RoutingResult {
    private String intent;
    private double confidence;
    private Map<String, Object> entities;
    private String priority;
    private String sentiment;

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public Map<String, Object> getEntities() { return entities; }
    public void setEntities(Map<String, Object> entities) { this.entities = entities; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public static RoutingResult fallback() {
        RoutingResult r = new RoutingResult();
        r.intent = "create_ticket";
        r.confidence = 0.0;
        r.priority = "normal";
        r.sentiment = "neutral";
        return r;
    }
}