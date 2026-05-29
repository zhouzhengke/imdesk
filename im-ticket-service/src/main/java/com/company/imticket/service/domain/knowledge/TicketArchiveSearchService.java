package com.company.imticket.service.domain.knowledge;

import org.springframework.stereotype.Service;

/**
 * Phase 1 stub for historical ticket archive search (L3 layer).
 * <p>
 * Placeholder that always returns {@code null}. Will be replaced with
 * Elasticsearch dense_vector search against closed ticket archives.
 */
@Service
public class TicketArchiveSearchService {

    /**
     * Search historical ticket archives for an answer to the given question.
     *
     * @param question the user's question
     * @return {@code null} in Phase 1 (placeholder for ES vector search)
     */
    public KnowledgeSearchResult search(String question) {
        return null;
    }
}
