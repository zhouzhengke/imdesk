package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单状态流水平表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_ticket_state_log")
public class TicketStateLog extends ImmutableBaseEntity {

    private Long ticketId;

    private String fromStatus;

    private String toStatus;

    private String operatorId;

    private String operatorType;

    private String remark;
}