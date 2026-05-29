package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_ticket")
public class Ticket extends BaseEntity {

    private String ticketNo;
    private Long capitalId;
    private String channel;
    private String channelUserId;
    private String channelGroupId;
    private String userName;
    private String description;
    private String status;
    private String priority;
    private String category;
    private String tags;
    private Long assignedAgentId;
    private String contextSummary;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Long getCapitalId() { return capitalId; }
    public void setCapitalId(Long capitalId) { this.capitalId = capitalId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChannelUserId() { return channelUserId; }
    public void setChannelUserId(String channelUserId) { this.channelUserId = channelUserId; }
    public String getChannelGroupId() { return channelGroupId; }
    public void setChannelGroupId(String channelGroupId) { this.channelGroupId = channelGroupId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Long getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(Long assignedAgentId) { this.assignedAgentId = assignedAgentId; }
    public String getContextSummary() { return contextSummary; }
    public void setContextSummary(String contextSummary) { this.contextSummary = contextSummary; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}