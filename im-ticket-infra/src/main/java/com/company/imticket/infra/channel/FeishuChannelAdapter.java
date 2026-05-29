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
public class FeishuChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(FeishuChannelAdapter.class);
    private final CapitalChannelMappingMapper capitalChannelMappingMapper;

    public FeishuChannelAdapter(CapitalChannelMappingMapper capitalChannelMappingMapper) {
        this.capitalChannelMappingMapper = capitalChannelMappingMapper;
    }

    @Override
    public ChannelType getType() {
        return ChannelType.FEISHU;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelMessage normalize(Map<String, Object> rawPayload) {
        Map<String, Object> event = (Map<String, Object>) rawPayload.getOrDefault("event", Map.of());
        Map<String, Object> sender = (Map<String, Object>) event.getOrDefault("sender", Map.of());
        Map<String, Object> message = (Map<String, Object>) event.getOrDefault("message", Map.of());

        ChannelMessage msg = new ChannelMessage();
        msg.setMessageId((String) rawPayload.getOrDefault("uuid", UUID.randomUUID().toString()));
        msg.setChannel("feishu");
        msg.setChannelUserId((String) sender.getOrDefault("open_id", ""));
        msg.setChannelGroupId((String) event.getOrDefault("chat_id", msg.getChannelUserId()));
        msg.setContent((String) message.getOrDefault("content", ""));
        msg.setContentType((String) message.getOrDefault("message_type", "text"));
        msg.setTimestamp(java.time.LocalDateTime.now());
        msg.setRawPayload(rawPayload);
        return msg;
    }

    @Override
    public void sendMessage(String channelUserId, String channelGroupId, String contentType, String content) {
        log.info("Feishu sendMessage: to={}, group={}, type={}, content={}",
                channelUserId, channelGroupId, contentType, content);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CapitalIdentity resolveCapital(Map<String, Object> rawPayload) {
        Map<String, Object> event = (Map<String, Object>) rawPayload.getOrDefault("event", Map.of());
        String tenantKey = (String) event.getOrDefault("tenant_key", "");
        CapitalChannelMapping mapping = capitalChannelMappingMapper
                .findByChannelAndIdentifier("feishu", tenantKey);
        if (mapping == null) {
            log.warn("未识别的飞书租户: {}", tenantKey);
            return null;
        }
        CapitalIdentity identity = new CapitalIdentity();
        identity.setCapitalId(mapping.getCapitalId());
        identity.setChannel("feishu");
        identity.setIdentifierType(mapping.getIdentifierType());
        identity.setIdentifierValue(mapping.getIdentifierValue());
        return identity;
    }

    @Override
    public boolean verifySignature(String signature, String timestamp, String nonce, String body) {
        return true;
    }
}