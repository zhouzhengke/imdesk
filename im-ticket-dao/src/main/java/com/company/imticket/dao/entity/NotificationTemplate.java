package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}