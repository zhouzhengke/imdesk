package com.company.imticket.service.domain.routing;

import com.company.imticket.common.enums.RoutingIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service for rule-based message routing.
 * <p>
 * Matches incoming user messages against a priority-ordered set of rules
 * (prefix, ticket number pattern, keyword) to determine the routing intent.
 * Returns null if no rule matches, allowing upstream callers to fall back
 * to LLM-based intent recognition.
 */
@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    /**
     * Regex for ticket numbers: IM-YYYYMMDD-NNNN (case-insensitive).
     */
    private static final Pattern TICKET_NO_PATTERN =
            Pattern.compile("IM-\\d{8}-\\d{4}", Pattern.CASE_INSENSITIVE);

    /**
     * Prefix rules. Checked in descending key-length order (longest first)
     * so that longer prefixes are never shadowed by shorter ones, regardless
     * of insertion order.
     */
    private static final Map<String, RoutingIntent> PREFIX_RULES = new LinkedHashMap<>();

    static {
        PREFIX_RULES.put("#查单", RoutingIntent.TICKET_QUERY);
        PREFIX_RULES.put("#我的工单", RoutingIntent.TICKET_QUERY);
        PREFIX_RULES.put("#帮助", RoutingIntent.CHITCHAT);
        PREFIX_RULES.put("#?", RoutingIntent.CHITCHAT);
        PREFIX_RULES.put("帮助", RoutingIntent.CHITCHAT);
        // "?" is placed after "#?" to avoid prematurely capturing "#?" as a "?" match
        PREFIX_RULES.put("?", RoutingIntent.CHITCHAT);
    }

    /**
     * Keyword rules. Order does not affect priority (keywords are checked only
     * after prefix and pattern rules have failed).
     */
    private static final Map<String, RoutingIntent> KEYWORD_RULES = new LinkedHashMap<>();

    static {
        KEYWORD_RULES.put("转人工", RoutingIntent.CREATE_TICKET);
        KEYWORD_RULES.put("联系客服", RoutingIntent.CREATE_TICKET);
        KEYWORD_RULES.put("创建工单", RoutingIntent.CREATE_TICKET);
        KEYWORD_RULES.put("人工客服", RoutingIntent.CREATE_TICKET);
    }

    /**
     * Match an incoming message against rule engine strategies.
     *
     * <p>Priority order:
     * <ol>
     *   <li>Prefix match — message starts with a known prefix</li>
     *   <li>Ticket number pattern — message contains a ticket number</li>
     *   <li>Keyword match — message contains a known keyword</li>
     * </ol>
     *
     * @param message the incoming user message (must not be null)
     * @return a {@link RuleMatchResult} if a rule matches, {@code null} otherwise
     */
    public RuleMatchResult match(String message) {
        if (message == null || message.isEmpty()) {
            log.debug("rule engine: empty or null message, no match");
            return null;
        }

        // 1. Prefix match (highest priority)
        RuleMatchResult prefixResult = matchPrefix(message);
        if (prefixResult != null) {
            log.debug("rule engine: prefix match, rule={}, intent={}", prefixResult.getMatchRule(), prefixResult.getIntent());
            return prefixResult;
        }

        // 2. Ticket number pattern match
        RuleMatchResult patternResult = matchTicketNoPattern(message);
        if (patternResult != null) {
            log.debug("rule engine: pattern match, rule={}, ticketNo={}", patternResult.getMatchRule(), patternResult.getExtractedParam());
            return patternResult;
        }

        // 3. Keyword match (lowest priority)
        RuleMatchResult keywordResult = matchKeyword(message);
        if (keywordResult != null) {
            log.debug("rule engine: keyword match, rule={}, intent={}", keywordResult.getMatchRule(), keywordResult.getIntent());
            return keywordResult;
        }

        log.debug("rule engine: no rule matched for message='{}'", message);
        return null;
    }

    private RuleMatchResult matchPrefix(String message) {
        return PREFIX_RULES.entrySet().stream()
                .sorted(Map.Entry.<String, RoutingIntent>comparingByKey(
                        Comparator.comparingInt(String::length).reversed()))
                .filter(entry -> message.startsWith(entry.getKey()))
                .findFirst()
                .map(entry -> {
                    String extractedParam = message.substring(entry.getKey().length());
                    return new RuleMatchResult(entry.getValue(), extractedParam, "prefix:" + entry.getKey());
                })
                .orElse(null);
    }

    private RuleMatchResult matchTicketNoPattern(String message) {
        Matcher matcher = TICKET_NO_PATTERN.matcher(message);
        if (matcher.find()) {
            String ticketNo = matcher.group();
            return new RuleMatchResult(RoutingIntent.TICKET_QUERY, ticketNo, "pattern:ticket_no");
        }
        return null;
    }

    private RuleMatchResult matchKeyword(String message) {
        for (Map.Entry<String, RoutingIntent> entry : KEYWORD_RULES.entrySet()) {
            String keyword = entry.getKey();
            if (message.contains(keyword)) {
                return new RuleMatchResult(entry.getValue(), message, "keyword:" + keyword);
            }
        }
        return null;
    }
}