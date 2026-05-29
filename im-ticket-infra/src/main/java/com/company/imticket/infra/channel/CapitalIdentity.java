package com.company.imticket.infra.channel;

public class CapitalIdentity {
    private Long capitalId;
    private String capitalName;
    private String channel;
    private String identifierType;
    private String identifierValue;

    public Long getCapitalId() { return capitalId; }
    public void setCapitalId(Long capitalId) { this.capitalId = capitalId; }
    public String getCapitalName() { return capitalName; }
    public void setCapitalName(String capitalName) { this.capitalName = capitalName; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getIdentifierValue() { return identifierValue; }
    public void setIdentifierValue(String identifierValue) { this.identifierValue = identifierValue; }
}