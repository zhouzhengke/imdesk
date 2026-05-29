package com.company.imticket.service.application.routing;

import com.company.imticket.common.enums.RoutingIntent;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.company.imticket.service.domain.routing.RuleMatchResult;

/**
 * Value object representing the final routing decision after the full routing pipeline.
 */
public class RouteDecision {
    private RoutingIntent intent;
    private String source;
    private double confidence;
    private String priority;
    private String sentiment;
    private String extractedParam;

    public RoutingIntent getIntent() { return intent; }
    public void setIntent(RoutingIntent intent) { this.intent = intent; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public String getExtractedParam() { return extractedParam; }
    public void setExtractedParam(String extractedParam) { this.extractedParam = extractedParam; }

    public static RouteDecision fromRule(RuleMatchResult rule) {
        RouteDecision d = new RouteDecision();
        d.intent = rule.getIntent();
        d.source = "RULE";
        d.confidence = 1.0;
        d.priority = "normal";
        d.extractedParam = rule.getExtractedParam();
        return d;
    }

    public static RouteDecision fromLLM(RoutingResult llm) {
        RouteDecision d = new RouteDecision();
        d.intent = RoutingIntent.valueOf(llm.getIntent().toUpperCase());
        d.source = "LLM";
        d.confidence = llm.getConfidence();
        d.priority = llm.getPriority() != null ? llm.getPriority() : "normal";
        d.sentiment = llm.getSentiment();
        return d;
    }

    public static RouteDecision fallback() {
        RouteDecision d = new RouteDecision();
        d.intent = RoutingIntent.CREATE_TICKET;
        d.source = "FALLBACK";
        d.confidence = 0.0;
        d.priority = "normal";
        return d;
    }
}
