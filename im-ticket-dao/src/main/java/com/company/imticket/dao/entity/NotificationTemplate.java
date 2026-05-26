package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_notification_template")
public class NotificationTemplate extends BaseEntity {

    private String code;

    private String name;

    private String direction;

    private String channel;

    private String format;

    private String title;

    private String content;

    private Integer enabled;
}