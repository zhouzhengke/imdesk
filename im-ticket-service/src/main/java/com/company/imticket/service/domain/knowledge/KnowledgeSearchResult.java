package com.company.imticket.service.domain.knowledge;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing the result of a knowledge base search.
 * <p>
 * Contains the answer text, the source layer that produced it,
 * reference documents or FAQ entries, and a confidence score.
 */
public class KnowledgeSearchResult {

    public static final String SOURCE_L1_FAQ = "L1_FAQ";
    public static final String SOURCE_L2_DOC = "L2_DOC";
    public static final String SOURCE_L3_ARCHIVE = "L3_ARCHIVE";

    private final String answer;
    private final String source;
    private final List<String> references;
    private final double confidence;

    public KnowledgeSearchResult(String answer, String source, List<String> references, double confidence) {
        this.answer = answer;
        this.source = source;
        this.references = references != null ? Collections.unmodifiableList(references) : Collections.emptyList();
        this.confidence = confidence;
    }

    public String getAnswer() {
        return answer;
    }

    public String getSource() {
        return source;
    }

    public List<String> getReferences() {
        return references;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeSearchResult)) return false;
        KnowledgeSearchResult that = (KnowledgeSearchResult) o;
        return Double.compare(that.confidence, confidence) == 0
                && Objects.equals(answer, that.answer)
                && Objects.equals(source, that.source)
                && Objects.equals(references, that.references);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer, source, references, confidence);
    }

    @Override
    public String toString() {
        return "KnowledgeSearchResult{source='" + source + "', confidence=" + confidence
                + ", references=" + references.size() + " items}";
    }
}
