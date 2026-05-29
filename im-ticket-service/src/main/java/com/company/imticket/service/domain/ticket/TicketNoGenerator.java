package com.company.imticket.service.domain.ticket;

import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Domain service for generating unique ticket numbers.
 * <p>
 * Uses Redis atomic increment to ensure uniqueness across multiple application instances.
 * Format: IM-YYYYMMDD-XXXX (e.g., IM-20260527-0001).
 */
@Service
public class TicketNoGenerator {

    private static final Logger log = LoggerFactory.getLogger(TicketNoGenerator.class);

    private static final String KEY_PREFIX = "im:ticket:seq:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    public TicketNoGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a unique ticket number.
     * <p>
     * The date part is based on the current date, and the sequence part is an atomic
     * increment from Redis. The key resets daily because the date part is embedded in
     * the Redis key, so the sequence starts from 1 each day.
     *
     * @return ticket number in format IM-YYYYMMDD-XXXX
     */
    private static final long MAX_DAILY_SEQUENCE = 9999L;

    public String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String key = KEY_PREFIX + datePart;
        Long seq = redisTemplate.opsForValue().increment(key);

        // Set TTL on new keys to prevent indefinite accumulation of old date keys
        if (seq == 1L) {
            redisTemplate.expire(key, Duration.ofHours(48));
        }

        // Guard against sequence overflow
        if (seq > MAX_DAILY_SEQUENCE) {
            throw new TicketException(BizErrorCode.PARAM_INVALID,
                    "Daily ticket limit exceeded: " + seq);
        }

        String ticketNo = String.format("IM-%s-%04d", datePart, seq);

        log.debug("generated ticket number: {}", ticketNo);
        return ticketNo;
    }
}
