package com.company.imticket.infra.channel;

import com.company.imticket.common.enums.ChannelType;
import java.util.Map;

public interface ChannelAdapter {
    ChannelType getType();
    ChannelMessage normalize(Map<String, Object> rawPayload);
    void sendMessage(String channelUserId, String channelGroupId, String contentType, String content);
    CapitalIdentity resolveCapital(Map<String, Object> rawPayload);
    boolean verifySignature(String signature, String timestamp, String nonce, String body);
}