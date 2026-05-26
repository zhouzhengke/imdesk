package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
}