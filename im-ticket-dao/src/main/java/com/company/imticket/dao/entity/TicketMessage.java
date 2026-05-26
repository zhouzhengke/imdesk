package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单消息表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_ticket_message")
public class TicketMessage extends ImmutableBaseEntity {

    private Long ticketId;

    private String senderType;

    private String senderId;

    private String senderName;

    private String content;

    private String contentType;
}