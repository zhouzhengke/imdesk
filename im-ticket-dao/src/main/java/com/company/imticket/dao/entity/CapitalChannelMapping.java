package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("im_capital_channel")
public class CapitalChannelMapping extends BaseEntity {

    private Long capitalId;
    private String channel;
    private String identifierType;
    private String identifierValue;

    public Long getCapitalId() { return capitalId; }
    public void setCapitalId(Long capitalId) { this.capitalId = capitalId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getIdentifierValue() { return identifierValue; }
    public void setIdentifierValue(String identifierValue) { this.identifierValue = identifierValue; }
}