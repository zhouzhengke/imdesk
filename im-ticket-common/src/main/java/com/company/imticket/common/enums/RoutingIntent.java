package com.company.imticket.common.enums;

public enum RoutingIntent {
    TICKET_QUERY("工单查询"),
    CREATE_TICKET("创建工单"),
    SUPPLEMENT("补充说明"),
    KNOWLEDGE_QUERY("知识库问答"),
    BUSINESS_QUERY("业务查询"),
    CHITCHAT("寒暄"),
    URGENT_ESCALATION("紧急升级");

    private final String label;

    RoutingIntent(String label) { this.label = label; }

    public String getLabel() { return label; }
}