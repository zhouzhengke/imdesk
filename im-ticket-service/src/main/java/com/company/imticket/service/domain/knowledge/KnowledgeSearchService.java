package com.company.imticket.service.domain.knowledge;

import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.BizException;
import com.company.imticket.dao.entity.KnowledgeFaq;
import com.company.imticket.dao.mapper.KnowledgeFaqMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Domain service for knowledge base search with three-layer retrieval.
 * <p>
 * Retrieval layers:
 * <ol>
 *   <li><b>L1 FAQ</b> — exact/fulltext match against the FAQ table via {@code KnowledgeFaqMapper}</li>
 *   <li><b>L2 Document RAG</b> — vector search against knowledge documents (Phase 1 stub)</li>
 *   <li><b>L3 Historical tickets</b> — vector search against closed ticket archives (Phase 1 stub)</li>
 * </ol>
 * Returns {@code null} when no layer produces a result.
 */
@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeFaqMapper faqMapper;
    private final DocumentSearchService documentSearchService;
    private final TicketArchiveSearchService archiveSearchService;

    public KnowledgeSearchService(KnowledgeFaqMapper faqMapper,
                                  DocumentSearchService documentSearchService,
                                  TicketArchiveSearchService archiveSearchService) {
        this.faqMapper = faqMapper;
        this.documentSearchService = documentSearchService;
        this.archiveSearchService = archiveSearchService;
    }

    /**
     * Search the knowledge base for an answer to the given question.
     * <p>
     * Tries L1 FAQ first, then L2 Document RAG, then L3 Historical tickets.
     * Logs each layer hit/miss at INFO level. Returns {@code null} if no layer
     * produces a result.
     *
     * @param question the user's question (must not be null)
     * @return a {@link KnowledgeSearchResult} if an answer is found, {@code null} otherwise
     * @throws BizException if question is null
     */
    public KnowledgeSearchResult search(String question) {
        if (question == null) {
            throw new BizException(BizErrorCode.PARAM_INVALID, "question must not be null");
        }

        // L1: FAQ exact/fulltext match
        List<KnowledgeFaq> faqResults = faqMapper.searchByQuestion(question);
        if (faqResults != null && !faqResults.isEmpty()) {
            KnowledgeFaq hit = faqResults.get(0);
            faqMapper.incrementHitCount(hit.getId());
            KnowledgeSearchResult result = new KnowledgeSearchResult(
                    hit.getAnswer(),
                    KnowledgeSearchResult.SOURCE_L1_FAQ,
                    Collections.singletonList(hit.getQuestion()),
                    1.0
            );
            log.info("knowledge search: L1_FAQ hit, question='{}', faqId={}, confidence=1.0",
                    question, hit.getId());
            return result;
        }
        log.info("knowledge search: L1_FAQ miss, question='{}'", question);

        // L2: Document RAG (Phase 1 stub — returns null)
        KnowledgeSearchResult docResult = documentSearchService.search(question);
        if (docResult != null) {
            log.info("knowledge search: L2_DOC hit, question='{}', confidence={}",
                    question, docResult.getConfidence());
            return docResult;
        }
        log.info("knowledge search: L2_DOC miss, question='{}'", question);

        // L3: Historical ticket archives (Phase 1 stub — returns null)
        KnowledgeSearchResult archiveResult = archiveSearchService.search(question);
        if (archiveResult != null) {
            log.info("knowledge search: L3_ARCHIVE hit, question='{}', confidence={}",
                    question, archiveResult.getConfidence());
            return archiveResult;
        }
        log.info("knowledge search: L3_ARCHIVE miss, question='{}'", question);

        // All layers missed
        log.info("knowledge search: all layers miss, question='{}'", question);
        return null;
    }
}
