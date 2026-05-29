package com.company.imticket.api;

import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.service.domain.duty.AssignmentResult;
import com.company.imticket.service.domain.duty.DutyAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E Journey 2: Knowledge-base escalation to ticket creation.
 * <p>
 * A user sends 3 consecutive KB questions that all miss the FAQ. On the 3rd miss,
 * the system triggers escalation and automatically creates a ticket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E Journey 2: KB Escalation → Ticket Creation")
class KnowledgeEscalationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @MockBean
    private AiClient aiClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private SessionCacheService sessionCache;

    @MockBean
    private DutyAssignmentService dutyAssignment;

    private final AtomicInteger kbRoundCounter = new AtomicInteger(0);

    private static final Long ESCALATION_AGENT_ID = 1L;
    private static final String ESCALATION_AGENT_NAME = "张三";

    @BeforeEach
    void setUp() {
        kbRoundCounter.set(0);

        // Return KNOWLEDGE_QUERY for all calls — none of the test messages
        // will match any rule engine prefix/keyword, so they fall through to LLM
        RoutingResult llmResult = new RoutingResult();
        llmResult.setIntent("knowledge_query");
        llmResult.setConfidence(0.9);
        llmResult.setPriority("normal");
        llmResult.setSentiment("neutral");
        when(aiClient.recognizeIntent(any())).thenReturn(llmResult);

        // Simulate Redis increment: each call to incrementKbRound returns
        // an increasing counter value. Note that both RoutingAppService.route()
        // and KnowledgeAppService.answer() call incrementKbRound, so each
        // webhook request results in 2 increments.
        when(sessionCache.incrementKbRound(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> kbRoundCounter.incrementAndGet());
        doNothing().when(sessionCache).resetKbRound(anyString(), anyString(), anyString());

        // Mock duty assignment: escalation tickets should be assigned to a duty agent
        AssignmentResult assignResult = new AssignmentResult(
                ESCALATION_AGENT_ID, ESCALATION_AGENT_NAME, "primary_duty");
        when(dutyAssignment.assign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(assignResult);
    }

    @Test
    @DisplayName("3 consecutive KB misses → escalation triggered → ticket created with agent assigned and notification sent")
    void threeConsecutiveKbMisses_shouldTriggerEscalationAndCreateTicket() throws Exception {
        String userId = "test_user_escalation";

        // Count tickets before
        long ticketCountBefore = ticketMapper.countPendingTickets();

        // Reset rabbitTemplate mock invocations to get clean verification
        reset(rabbitTemplate);

        // Send 3 KB questions that do NOT match any FAQ
        sendWebhook("这是一个完全不匹配FAQ的随机问题一", userId);
        sendWebhook("这也是一个完全不匹配FAQ的随机问题二", userId);
        sendWebhook("这还是一个完全不匹配FAQ的随机问题三", userId);

        // After 3 misses, escalation should have created a ticket
        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertTrue(ticketCountAfter > ticketCountBefore,
                "After 3 KB misses, escalation should create a ticket. "
                        + "before=" + ticketCountBefore + ", after=" + ticketCountAfter);

        // Verify KB round was reset after escalation
        verify(sessionCache, atLeastOnce()).resetKbRound(eq("wecom"), eq(userId), anyString());

        // Verify the created ticket has an assigned agent
        Ticket created = ticketMapper.findOpenTicketByChannelUser("wecom", userId);
        assertNotNull(created, "Escalation should create an open ticket");
        assertNotNull(created.getAssignedAgentId(),
                "Escalation ticket should be assigned to a duty agent");
        assertEquals(ESCALATION_AGENT_ID, created.getAssignedAgentId());

        // Verify notification was sent via RabbitTemplate.
        // TicketAppService.createTicket sends two notifications:
        //   - ticket_created_to_user (to the channel user)
        //   - ticket_assigned_to_agent (to the assigned agent)
        // Both go through NotificationSender → RabbitTemplate.convertAndSend
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("2 consecutive KB misses → should NOT trigger escalation")
    void twoConsecutiveKbMisses_shouldNotTriggerEscalation() throws Exception {
        String userId = "test_user_two_misses";

        long ticketCountBefore = ticketMapper.countPendingTickets();

        // Send only 2 KB questions
        sendWebhook("不匹配FAQ的随机问题A", userId);
        sendWebhook("不匹配FAQ的随机问题B", userId);

        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertEquals(ticketCountBefore, ticketCountAfter,
                "Only 2 KB misses should not trigger escalation (threshold is 3)");
    }

    @Test
    @DisplayName("KB miss → FAQ hit on next call → should NOT escalate")
    void kbMissThenFaqHit_shouldNotEscalate() throws Exception {
        String userId = "test_user_miss_then_hit";

        long ticketCountBefore = ticketMapper.countPendingTickets();

        // First: a miss
        sendWebhook("完全不匹配FAQ的问题", userId);

        // Second: a hit (this FAQ exists in test-data.sql)
        sendWebhook("审批流程是什么？", userId);

        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertEquals(ticketCountBefore, ticketCountAfter,
                "A FAQ hit should reset the miss counter, no escalation needed");
    }

    @Test
    @DisplayName("escalation creates ticket with correct channel, user info, and assigned agent")
    void escalation_shouldCreateTicketWithCorrectChannelInfo() throws Exception {
        String userId = "test_user_escalation_detail";

        sendWebhook("不匹配FAQ的问题X", userId);
        sendWebhook("不匹配FAQ的问题Y", userId);
        sendWebhook("不匹配FAQ的问题Z", userId);

        // Find the created ticket for this user
        Ticket created = ticketMapper.findOpenTicketByChannelUser("wecom", userId);
        assertNotNull(created, "Escalation should create an open ticket for the user");
        assertEquals("wecom", created.getChannel());
        assertEquals(userId, created.getChannelUserId());
        assertNotNull(created.getTicketNo(), "Ticket should have a generated ticket number");
        assertTrue(created.getTicketNo().startsWith("IM-"),
                "Ticket number should start with IM- prefix");
        assertNotNull(created.getAssignedAgentId(),
                "Escalation ticket should be assigned to a duty agent");
        assertEquals(ESCALATION_AGENT_ID, created.getAssignedAgentId());

        // Verify that duty assignment was called
        verify(dutyAssignment, atLeastOnce()).assign(
                eq("wecom"), eq(userId), anyString(), anyString());

        // Verify notification was published to RabbitMQ
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(), anyString(), any(Object.class));
    }

    private void sendWebhook(String content, String fromUserName) throws Exception {
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "Content", content,
                "FromUserName", fromUserName,
                "MsgType", "text"
        ));

        mockMvc.perform(post("/api/v1/webhook/wecom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }
}
