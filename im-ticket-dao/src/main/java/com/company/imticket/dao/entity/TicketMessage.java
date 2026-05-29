package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("im_ticket_message")
public class TicketMessage extends ImmutableBaseEntity {

    private Long ticketId;
    private String senderType;
    private String senderId;
    private String senderName;
    private String content;
    private String contentType;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}