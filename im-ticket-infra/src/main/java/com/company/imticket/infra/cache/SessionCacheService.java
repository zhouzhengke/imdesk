package com.company.imticket.infra.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SessionCacheService {

    private final StringRedisTemplate redisTemplate;

    public SessionCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int incrementKbRound(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":kb_rounds";
        Long rounds = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        return rounds != null ? rounds.intValue() : 0;
    }

    public void resetKbRound(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":kb_rounds";
        redisTemplate.delete(key);
    }

    public void cacheLastAgent(String channel, String userId, String groupId, Long agentId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":last_agent";
        redisTemplate.opsForValue().set(key, String.valueOf(agentId), 7, TimeUnit.DAYS);
    }

    public Long getLastAgent(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":last_agent";
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.valueOf(val) : null;
    }
}