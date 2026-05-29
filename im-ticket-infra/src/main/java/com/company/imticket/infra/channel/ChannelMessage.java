package com.company.imticket.infra.channel;

import java.time.LocalDateTime;
import java.util.Map;

public class ChannelMessage {
    private String messageId;
    private String channel;
    private String channelUserId;
    private String channelGroupId;
    private String userName;
    private String content;
    private String contentType;
    private LocalDateTime timestamp;
    private Map<String, Object> rawPayload;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChannelUserId() { return channelUserId; }
    public void setChannelUserId(String channelUserId) { this.channelUserId = channelUserId; }
    public String getChannelGroupId() { return channelGroupId; }
    public void setChannelGroupId(String channelGroupId) { this.channelGroupId = channelGroupId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }
}