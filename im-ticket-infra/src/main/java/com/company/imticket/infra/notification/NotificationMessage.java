package com.company.imticket.infra.notification;

public class NotificationMessage {
    private String templateCode;
    private String channel;
    private String content;
    private String targetUserId;
    private String targetGroupId;

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(String targetGroupId) { this.targetGroupId = targetGroupId; }
}