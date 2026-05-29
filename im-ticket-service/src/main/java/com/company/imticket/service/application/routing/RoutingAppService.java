package com.company.imticket.service.application.routing;

import com.company.imticket.common.enums.RoutingIntent;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.ai.dto.IntentRecognitionContext;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.service.domain.routing.RuleEngineService;
import com.company.imticket.service.domain.routing.RuleMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service that orchestrates the full routing pipeline.
 * <p>
 * Pipeline: Rule engine (fast path) → LLM intent recognition (fallback) → default create_ticket (degraded).
 */
@Service
public class RoutingAppService {

    private static final Logger log = LoggerFactory.getLogger(RoutingAppService.class);

    private final RuleEngineService ruleEngine;
    private final AiClient aiClient;
    private final SessionCacheService sessionCache;

    public RoutingAppService(RuleEngineService ruleEngine, AiClient aiClient, SessionCacheService sessionCache) {
        this.ruleEngine = ruleEngine;
        this.aiClient = aiClient;
        this.sessionCache = sessionCache;
    }

    /**
     * Route an incoming channel message to determine the user's intent.
     *
     * @param message       the normalized channel message
     * @param capitalName   the identified capital/customer name
     * @param hasOpenTicket whether the user already has an open ticket
     * @return a {@link RouteDecision} with the determined intent and metadata
     */
    public RouteDecision route(ChannelMessage message, String capitalName, boolean hasOpenTicket) {
        // Step 1: Rule engine (fast path)
        RuleMatchResult ruleResult = ruleEngine.match(message.getContent());
        if (ruleResult != null) {
            log.info("route by rule: intent={}, rule={}, user={}",
                    ruleResult.getIntent(), ruleResult.getMatchRule(), message.getChannelUserId());
            return RouteDecision.fromRule(ruleResult);
        }

        // Step 2: LLM intent recognition
        try {
            int kbRounds = sessionCache.incrementKbRound(
                    message.getChannel(), message.getChannelUserId(), message.getChannelGroupId());

            IntentRecognitionContext ctx = IntentRecognitionContext.builder()
                    .capitalName(capitalName)
                    .userName(message.getUserName())
                    .hasOpenTicket(hasOpenTicket)
                    .kbInteractionCount(kbRounds)
                    .message(message.getContent())
                    .build();

            RoutingResult aiResult = aiClient.recognizeIntent(ctx);
            log.info("route by LLM: intent={}, confidence={}, priority={}, user={}",
                    aiResult.getIntent(), aiResult.getConfidence(), aiResult.getPriority(),
                    message.getChannelUserId());
            return RouteDecision.fromLLM(aiResult);

        } catch (Exception e) {
            // Step 3: LLM failure → fallback to default create_ticket
            log.error("LLM routing failed for user={}, falling back to create_ticket",
                    message.getChannelUserId(), e);
            return RouteDecision.fallback();
        }
    }

    /**
     * Check whether the routing decision indicates a knowledge-base query.
     */
    public boolean isKnowledgeQuery(RouteDecision decision) {
        return decision.getIntent() == RoutingIntent.KNOWLEDGE_QUERY
                || decision.getIntent() == RoutingIntent.CHITCHAT;
    }

    /**
     * Check whether the routing decision should create a ticket.
     */
    public boolean shouldCreateTicket(RouteDecision decision) {
        return decision.getIntent() == RoutingIntent.CREATE_TICKET
                || decision.getIntent() == RoutingIntent.URGENT_ESCALATION
                || decision.getIntent() == RoutingIntent.SUPPLEMENT;
    }
}
