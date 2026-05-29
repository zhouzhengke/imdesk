package com.company.imticket.common.enums;

public enum ChannelType {
    WECOM("wecom", "企业微信"),
    FEISHU("feishu", "飞书");

    private final String code;
    private final String label;

    ChannelType(String code, String label) { this.code = code; this.label = label; }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}