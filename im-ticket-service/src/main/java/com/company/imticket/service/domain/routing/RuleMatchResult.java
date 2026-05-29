package com.company.imticket.service.domain.routing;

import com.company.imticket.common.enums.RoutingIntent;

import java.util.Objects;

/**
 * Value object representing the result of rule-based message matching.
 */
public class RuleMatchResult {

    private final RoutingIntent intent;
    private final String extractedParam;
    private final String matchRule;

    public RuleMatchResult(RoutingIntent intent, String extractedParam, String matchRule) {
        this.intent = intent;
        this.extractedParam = extractedParam;
        this.matchRule = matchRule;
    }

    public RoutingIntent getIntent() {
        return intent;
    }

    public String getExtractedParam() {
        return extractedParam;
    }

    public String getMatchRule() {
        return matchRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleMatchResult)) return false;
        RuleMatchResult that = (RuleMatchResult) o;
        return intent == that.intent
                && Objects.equals(extractedParam, that.extractedParam)
                && Objects.equals(matchRule, that.matchRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intent, extractedParam, matchRule);
    }

    @Override
    public String toString() {
        return "RuleMatchResult{intent=" + intent + ", extractedParam='" + extractedParam + "', matchRule='" + matchRule + "'}";
    }
}