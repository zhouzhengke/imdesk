package com.company.imticket.common.enums;

import java.util.Set;

public enum TicketStatus {
    PENDING("待处理"),
    IN_PROGRESS("处理中"),
    TRANSFERRED("已转交"),
    REJECTED("已驳回"),
    DEFERRED("已延期"),
    RESOLVED("已解决"),
    WAITING_CONFIRM("待确认"),
    CLOSED("已关闭");

    private final String label;

    TicketStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isTerminal() { return this == CLOSED; }

    public boolean canTransition(TicketStatus target) {
        return switch (this) {
            case PENDING -> Set.of(IN_PROGRESS).contains(target);
            case IN_PROGRESS -> Set.of(RESOLVED, TRANSFERRED, REJECTED, DEFERRED).contains(target);
            case TRANSFERRED -> Set.of(IN_PROGRESS, PENDING).contains(target);
            case REJECTED -> Set.of(PENDING).contains(target);
            case DEFERRED -> Set.of(IN_PROGRESS).contains(target);
            case RESOLVED -> Set.of(WAITING_CONFIRM, CLOSED).contains(target);
            case WAITING_CONFIRM -> Set.of(CLOSED, IN_PROGRESS).contains(target);
            case CLOSED -> false;
        };
    }
}