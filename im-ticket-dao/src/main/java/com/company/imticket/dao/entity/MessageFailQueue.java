package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息失败队列表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_message_fail_queue")
public class MessageFailQueue extends BaseEntity {

    private Long ticketId;

    private String channel;

    private String channelUserId;

    private String channelGroupId;

    private String content;

    private String contentType;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime nextRetryAt;

    private String status;

    private String errorMsg;
}