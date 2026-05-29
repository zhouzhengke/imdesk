package com.company.imticket.infra.ai.dto;

public class IntentRecognitionContext {
    private String capitalName;
    private String userName;
    private boolean hasOpenTicket;
    private int kbInteractionCount;
    private String message;

    public String getCapitalName() { return capitalName; }
    public void setCapitalName(String capitalName) { this.capitalName = capitalName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public boolean isHasOpenTicket() { return hasOpenTicket; }
    public void setHasOpenTicket(boolean hasOpenTicket) { this.hasOpenTicket = hasOpenTicket; }
    public int getKbInteractionCount() { return kbInteractionCount; }
    public void setKbInteractionCount(int kbInteractionCount) { this.kbInteractionCount = kbInteractionCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static IntentRecognitionContextBuilder builder() {
        return new IntentRecognitionContextBuilder();
    }

    public static class IntentRecognitionContextBuilder {
        private String capitalName;
        private String userName;
        private boolean hasOpenTicket;
        private int kbInteractionCount;
        private String message;

        public IntentRecognitionContextBuilder capitalName(String capitalName) {
            this.capitalName = capitalName;
            return this;
        }
        public IntentRecognitionContextBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }
        public IntentRecognitionContextBuilder hasOpenTicket(boolean hasOpenTicket) {
            this.hasOpenTicket = hasOpenTicket;
            return this;
        }
        public IntentRecognitionContextBuilder kbInteractionCount(int kbInteractionCount) {
            this.kbInteractionCount = kbInteractionCount;
            return this;
        }
        public IntentRecognitionContextBuilder message(String message) {
            this.message = message;
            return this;
        }
        public IntentRecognitionContext build() {
            IntentRecognitionContext ctx = new IntentRecognitionContext();
            ctx.capitalName = this.capitalName;
            ctx.userName = this.userName;
            ctx.hasOpenTicket = this.hasOpenTicket;
            ctx.kbInteractionCount = this.kbInteractionCount;
            ctx.message = this.message;
            return ctx;
        }
    }
}