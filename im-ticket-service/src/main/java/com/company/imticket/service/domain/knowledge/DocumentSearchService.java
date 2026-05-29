package com.company.imticket.service.domain.knowledge;

import org.springframework.stereotype.Service;

/**
 * Phase 1 stub for document-based knowledge retrieval (L2 layer).
 * <p>
 * Placeholder that always returns {@code null}. Will be replaced with
 * Elasticsearch dense_vector search once document indexing is implemented.
 */
@Service
public class DocumentSearchService {

    /**
     * Search knowledge documents for an answer to the given question.
     *
     * @param question the user's question
     * @return {@code null} in Phase 1 (placeholder for ES vector search)
     */
    public KnowledgeSearchResult search(String question) {
        return null;
    }
}
