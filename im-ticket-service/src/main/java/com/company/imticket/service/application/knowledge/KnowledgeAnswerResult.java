package com.company.imticket.service.application.knowledge;

/**
 * Value object representing the result of a knowledge-base Q&amp;A interaction.
 */
public class KnowledgeAnswerResult {
    private boolean answered;
    private boolean shouldEscalate;
    private String answer;
    private String source;

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }
    public boolean isShouldEscalate() { return shouldEscalate; }
    public void setShouldEscalate(boolean shouldEscalate) { this.shouldEscalate = shouldEscalate; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public static KnowledgeAnswerResult answered(String answer, String source) {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.answered = true;
        r.answer = answer;
        r.source = source;
        return r;
    }

    public static KnowledgeAnswerResult noAnswer() {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.answered = false;
        return r;
    }

    public static KnowledgeAnswerResult escalate() {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.shouldEscalate = true;
        return r;
    }
}
