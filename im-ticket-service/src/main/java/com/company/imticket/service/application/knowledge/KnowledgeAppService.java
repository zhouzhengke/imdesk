package com.company.imticket.service.application.knowledge;

import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.service.domain.knowledge.KnowledgeSearchResult;
import com.company.imticket.service.domain.knowledge.KnowledgeSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Application service for knowledge-base Q&amp;A with escalation logic.
 * <p>
 * Orchestrates the three-layer knowledge retrieval pipeline and uses LLM to generate
 * a final answer. Tracks consecutive KB interactions per session and triggers escalation
 * to ticket creation when the threshold is exceeded.
 */
@Service
public class KnowledgeAppService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAppService.class);

    private final KnowledgeSearchService searchService;
    private final AiClient aiClient;
    private final SessionCacheService sessionCache;

    @Value("${im.knowledge.escalation-rounds:3}")
    private int escalationRounds;

    public KnowledgeAppService(KnowledgeSearchService searchService,
                               AiClient aiClient,
                               SessionCacheService sessionCache) {
        this.searchService = searchService;
        this.aiClient = aiClient;
        this.sessionCache = sessionCache;
    }

    /**
     * Answer a user question using the knowledge base.
     * <p>
     * Performs three-layer retrieval (FAQ → Document RAG → Historical tickets), then uses
     * LLM to generate a polished answer. If no answer is found across all layers, checks
     * whether the escalation threshold has been reached.
     *
     * @param channel  IM channel identifier
     * @param userId   channel user ID
     * @param groupId  channel group/chat ID
     * @param question the user's question
     * @return a {@link KnowledgeAnswerResult} with the answer, or escalation/fallback flags
     */
    public KnowledgeAnswerResult answer(String channel, String userId, String groupId, String question) {
        KnowledgeSearchResult searchResult = searchService.search(question);

        if (searchResult == null) {
            int rounds = sessionCache.incrementKbRound(channel, userId, groupId);
            log.info("KB all layers miss: question='{}', rounds={}, threshold={}",
                    question, rounds, escalationRounds);
            if (rounds >= escalationRounds) {
                log.info("KB escalation triggered: rounds={} >= threshold={}, user={}",
                        rounds, escalationRounds, userId);
                sessionCache.resetKbRound(channel, userId, groupId);
                return KnowledgeAnswerResult.escalate();
            }
            return KnowledgeAnswerResult.noAnswer();
        }

        String finalAnswer = aiClient.generateKnowledgeAnswer(question, searchResult.getAnswer());
        log.info("KB answer generated: source={}, confidence={}, question='{}'",
                searchResult.getSource(), searchResult.getConfidence(), question);
        return KnowledgeAnswerResult.answered(finalAnswer, searchResult.getSource());
    }
}
