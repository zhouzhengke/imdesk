package com.company.imticket.service.domain.routing;

import com.company.imticket.common.enums.RoutingIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RuleEngineService}.
 * <p>
 * No Spring container — pure unit tests with direct instantiation.
 */
class RuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService();
    }

    // ======================== Prefix match tests ========================

    @Test
    void match_prefixHashtagChaDan_shouldReturnTicketQuery() {
        RuleMatchResult result = ruleEngineService.match("#查单 帮我查一下订单状态");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("帮我查一下订单状态", result.getExtractedParam().trim());
        assertEquals("prefix:#查单", result.getMatchRule());
    }

    @Test
    void match_prefixHashtagChaDanNoContent_shouldReturnTicketQueryWithEmptyParam() {
        RuleMatchResult result = ruleEngineService.match("#查单");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("", result.getExtractedParam());
        assertEquals("prefix:#查单", result.getMatchRule());
    }

    @Test
    void match_prefixHashtagMyTickets_shouldReturnTicketQuery() {
        RuleMatchResult result = ruleEngineService.match("#我的工单 查看我的工单列表");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("prefix:#我的工单", result.getMatchRule());
    }

    @Test
    void match_prefixHashtagHelp_shouldReturnChitchat() {
        RuleMatchResult result = ruleEngineService.match("#帮助 如何使用系统");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:#帮助", result.getMatchRule());
    }

    @Test
    void match_prefixHashtagQuestionMark_shouldReturnChitchat() {
        RuleMatchResult result = ruleEngineService.match("#? 这是什么");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:#?", result.getMatchRule());
    }

    @Test
    void match_prefixBangZhu_shouldReturnChitchat() {
        RuleMatchResult result = ruleEngineService.match("帮助 我的订单");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:帮助", result.getMatchRule());
    }

    @Test
    void match_prefixQuestionMark_shouldReturnChitchat() {
        // This must come after "#?" check, so a lone "? something" matches "?" prefix
        RuleMatchResult result = ruleEngineService.match("? 有没有客服在");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:?", result.getMatchRule());
    }

    // ======================== Ticket number pattern match tests ========================

    @Test
    void match_patternTicketNo_shouldReturnTicketQuery() {
        RuleMatchResult result = ruleEngineService.match("帮我查一下工单 IM-20260526-0001 的状态");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("IM-20260526-0001", result.getExtractedParam());
        assertEquals("pattern:ticket_no", result.getMatchRule());
    }

    @Test
    void match_patternTicketNoLowercase_shouldReturnTicketQuery() {
        RuleMatchResult result = ruleEngineService.match("查 im-20260526-0001");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("im-20260526-0001", result.getExtractedParam());
    }

    @Test
    void match_patternTicketNoIsolated_shouldReturnTicketQuery() {
        RuleMatchResult result = ruleEngineService.match("IM-20260526-0001");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("IM-20260526-0001", result.getExtractedParam());
    }

    @Test
    void match_patternTicketNoWrongFormat_shouldNotMatch() {
        // Only 3 digits at the end instead of 4
        RuleMatchResult result = ruleEngineService.match("查 IM-20260526-000");

        assertNull(result);
    }

    @Test
    void match_patternTicketNoWrongDateLength_shouldNotMatch() {
        // 9 digits in the date part
        RuleMatchResult result = ruleEngineService.match("查 IM-202605261-0001");

        assertNull(result);
    }

    @Test
    void match_patternTicketNoMissingPrefix_shouldNotMatch() {
        // Wrong prefix
        RuleMatchResult result = ruleEngineService.match("TK-20260526-0001");

        assertNull(result);
    }

    @Test
    void match_patternTicketNoMultipleMatches_shouldReturnFirstMatch() {
        RuleMatchResult result = ruleEngineService.match("对比 IM-20260526-0001 和 IM-20260526-0002");

        assertNotNull(result);
        assertEquals("IM-20260526-0001", result.getExtractedParam());
    }

    // ======================== Keyword match tests ========================

    @Test
    void match_keywordZhuanRenGong_shouldReturnCreateTicket() {
        RuleMatchResult result = ruleEngineService.match("我要转人工");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
        assertEquals("我要转人工", result.getExtractedParam());
        assertEquals("keyword:转人工", result.getMatchRule());
    }

    @Test
    void match_keywordLianXiKeFu_shouldReturnCreateTicket() {
        RuleMatchResult result = ruleEngineService.match("我想联系客服");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
        assertEquals("keyword:联系客服", result.getMatchRule());
    }

    @Test
    void match_keywordChuangJianGongDan_shouldReturnCreateTicket() {
        RuleMatchResult result = ruleEngineService.match("请帮我创建工单");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
        assertEquals("keyword:创建工单", result.getMatchRule());
    }

    @Test
    void match_keywordRenGongKeFu_shouldReturnCreateTicket() {
        RuleMatchResult result = ruleEngineService.match("需要人工客服协助");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
        assertEquals("keyword:人工客服", result.getMatchRule());
    }

    @Test
    void match_multipleKeywords_shouldReturnFirstInInsertionOrder() {
        // "转人工" comes before "人工客服" in KEYWORD_RULES insertion order
        RuleMatchResult result = ruleEngineService.match("转人工 人工客服");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
        assertEquals("keyword:转人工", result.getMatchRule());
    }

    // ======================== No match tests ========================

    @Test
    void match_noMatchingRule_shouldReturnNull() {
        RuleMatchResult result = ruleEngineService.match("今天天气真好");

        assertNull(result);
    }

    @Test
    void match_randomBusinessQuery_shouldReturnNull() {
        RuleMatchResult result = ruleEngineService.match("审批流程是什么");

        assertNull(result);
    }

    // ======================== Priority tests ========================

    @Test
    void match_prefixBeatsKeyword_shouldReturnPrefixResult() {
        // "#帮助" prefix should win over "转人工" keyword inside the same message
        RuleMatchResult result = ruleEngineService.match("#帮助转人工");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:#帮助", result.getMatchRule());
        assertEquals("转人工", result.getExtractedParam());
    }

    @Test
    void match_prefixBeatsPattern_shouldReturnPrefixResult() {
        // "#查单" prefix should win over ticket number pattern inside the same message
        RuleMatchResult result = ruleEngineService.match("#查单 IM-20260526-0001");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("prefix:#查单", result.getMatchRule());
    }

    @Test
    void match_patternBeatsKeyword_shouldReturnPatternResult() {
        // Ticket number pattern should win over keyword inside the same message
        RuleMatchResult result = ruleEngineService.match("转人工查工单 IM-20260526-0001");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("pattern:ticket_no", result.getMatchRule());
        assertEquals("IM-20260526-0001", result.getExtractedParam());
    }

    // ======================== Similar-but-different patterns ========================

    @Test
    void match_similarButDifferentPrefix_shouldNotMatch() {
        // "#查" is not a registered prefix
        RuleMatchResult result = ruleEngineService.match("#查 订单");

        assertNull(result);
    }

    @Test
    void match_similarButDifferentHashtag_shouldNotMatch() {
        // "#人工" is not a registered prefix
        RuleMatchResult result = ruleEngineService.match("#人工 客服");

        assertNull(result);
    }

    @Test
    void match_keywordAsSubstringOfLongerWord_shouldMatch() {
        // "转人工" is inside "请帮我转人工啊" — should match because contains works
        RuleMatchResult result = ruleEngineService.match("请帮我转人工啊");

        assertNotNull(result);
        assertEquals(RoutingIntent.CREATE_TICKET, result.getIntent());
    }

    @Test
    void match_hashtagPrefixInLongerMessage_shouldMatch() {
        // "#帮助文档" — only "#帮助" is a prefix, and the message starts with "#帮助文档"
        // "#帮助" is a prefix, so it SHOULD match (startsWith("#帮助") on "#帮助文档" is true)
        RuleMatchResult result = ruleEngineService.match("#帮助文档 怎么用");

        assertNotNull(result);
        assertEquals(RoutingIntent.CHITCHAT, result.getIntent());
        assertEquals("prefix:#帮助", result.getMatchRule());
        assertEquals("文档 怎么用", result.getExtractedParam());
    }

    // ======================== Edge cases ========================

    @Test
    void match_nullMessage_shouldReturnNull() {
        RuleMatchResult result = ruleEngineService.match(null);

        assertNull(result);
    }

    @Test
    void match_emptyMessage_shouldReturnNull() {
        RuleMatchResult result = ruleEngineService.match("");

        assertNull(result);
    }

    @Test
    void match_blankMessage_shouldReturnNull() {
        // " " is not empty and doesn't match any rule
        RuleMatchResult result = ruleEngineService.match("   ");

        assertNull(result);
    }

    @Test
    void match_longPrefixExactMatch_shouldSucceed() {
        // The longer prefix "#我的工单" should be matched, not "#查单" or shorter prefix
        // "#我的工单" does not start with "#查单" or any other prefix, so it's fine
        RuleMatchResult result = ruleEngineService.match("#我的工单");

        assertNotNull(result);
        assertEquals(RoutingIntent.TICKET_QUERY, result.getIntent());
        assertEquals("prefix:#我的工单", result.getMatchRule());
        assertEquals("", result.getExtractedParam());
    }
}