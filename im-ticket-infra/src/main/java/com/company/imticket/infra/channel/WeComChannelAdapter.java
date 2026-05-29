package com.company.imticket.infra.channel;

import com.company.imticket.common.enums.ChannelType;
import com.company.imticket.dao.entity.CapitalChannelMapping;
import com.company.imticket.dao.mapper.CapitalChannelMappingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class WeComChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(WeComChannelAdapter.class);
    private final CapitalChannelMappingMapper capitalChannelMappingMapper;

    public WeComChannelAdapter(CapitalChannelMappingMapper capitalChannelMappingMapper) {
        this.capitalChannelMappingMapper = capitalChannelMappingMapper;
    }

    @Override
    public ChannelType getType() {
        return ChannelType.WECOM;
    }

    @Override
    public ChannelMessage normalize(Map<String, Object> rawPayload) {
        ChannelMessage msg = new ChannelMessage();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setChannel("wecom");
        msg.setChannelUserId((String) rawPayload.getOrDefault("FromUserName", ""));
        msg.setChannelGroupId((String) rawPayload.getOrDefault("ChatId", msg.getChannelUserId()));
        msg.setContent((String) rawPayload.getOrDefault("Content", ""));
        msg.setContentType((String) rawPayload.getOrDefault("MsgType", "text"));
        msg.setTimestamp(java.time.LocalDateTime.now());
        msg.setRawPayload(rawPayload);
        return msg;
    }

    @Override
    public void sendMessage(String channelUserId, String channelGroupId, String contentType, String content) {
        log.info("WeCom sendMessage: to={}, group={}, type={}, content={}",
                channelUserId, channelGroupId, contentType, content);
    }

    @Override
    public CapitalIdentity resolveCapital(Map<String, Object> rawPayload) {
        String externalUserId = (String) rawPayload.getOrDefault("ExternalUserId", "");
        CapitalChannelMapping mapping = capitalChannelMappingMapper
                .findByChannelAndIdentifier("wecom", externalUserId);
        if (mapping == null) {
            log.warn("未识别的企微用户: {}", externalUserId);
            return null;
        }
        CapitalIdentity identity = new CapitalIdentity();
        identity.setCapitalId(mapping.getCapitalId());
        identity.setChannel("wecom");
        identity.setIdentifierType(mapping.getIdentifierType());
        identity.setIdentifierValue(mapping.getIdentifierValue());
        return identity;
    }

    @Override
    public boolean verifySignature(String signature, String timestamp, String nonce, String body) {
        return true;
    }
}