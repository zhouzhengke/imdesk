package com.company.imticket.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketStateLog;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketStateLogMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.service.application.ticket.TicketAppService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E Journey 3: Ticket Full Lifecycle")
class TicketLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketAppService ticketAppService;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketStateLogMapper stateLogMapper;

    @MockBean
    private AiClient aiClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private SessionCacheService sessionCache;

    private Long ticketId;
    private static final Long AGENT_ID = 1L;
    private static final String AGENT_NAME = "张三";

    @BeforeEach
    void setUp() {
        when(aiClient.generateTicketSummary(anyString())).thenReturn("工单摘要：用户咨询账户问题，客服已解决。");
        when(aiClient.generateKnowledgeAnswer(anyString(), anyString())).thenReturn("参考回答");

        ChannelMessage msg = new ChannelMessage();
        msg.setChannel("wecom");
        msg.setChannelUserId("test_user_lifecycle");
        msg.setChannelGroupId("test_group_lifecycle");
        msg.setUserName("测试用户");
        msg.setContent("我需要帮助重置我的账户密码");
        msg.setContentType("text");

        Ticket ticket = ticketAppService.createTicket(msg, 1L, "XX银行", "normal", "账户密码重置");
        ticketId = ticket.getId();
        assertNotNull(ticketId, "Ticket should be created with a valid ID");
        assertEquals("PENDING", ticket.getStatus(), "New ticket should be in PENDING status");
    }

    // ======================== Phase 1: Agent Accept ========================

    @Test
    @DisplayName("PENDING → agent accept → IN_PROGRESS")
    void accept_pendingToInProgress_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(AGENT_ID));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals(AGENT_ID, updated.getAssignedAgentId());

        List<TicketStateLog> logs = stateLogMapper.selectList(
                new LambdaQueryWrapper<TicketStateLog>()
                        .eq(TicketStateLog::getTicketId, ticketId)
                        .orderByAsc(TicketStateLog::getCreatedAt));
        assertFalse(logs.isEmpty(), "State log should record the transition");
        TicketStateLog lastLog = logs.get(logs.size() - 1);
        assertEquals("PENDING", lastLog.getFromStatus());
        assertEquals("IN_PROGRESS", lastLog.getToStatus());
    }

    @Test
    @DisplayName("agent accept without assigned agent → should fail")
    void accept_withoutAssignedAgent_shouldFail() throws Exception {
        ChannelMessage msg = new ChannelMessage();
        msg.setChannel("wecom");
        msg.setChannelUserId("test_user_no_agent");
        msg.setChannelGroupId("test_group_no_agent");
        msg.setUserName("无分配用户");
        msg.setContent("问题描述");
        msg.setContentType("text");

        Ticket unassigned = ticketAppService.createTicket(msg, 1L, "XX银行", "normal", "无分配测试");
        unassigned.setAssignedAgentId(null);
        ticketMapper.updateById(unassigned);

        mockMvc.perform(post("/api/v1/tickets/{id}/accept", unassigned.getId())
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ======================== Phase 2: Agent Reply ========================

    @Test
    @DisplayName("agent reply → message persisted")
    void reply_shouldPersistAgentMessage() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");

        mockMvc.perform(post("/api/v1/tickets/{id}/reply", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("agentName", AGENT_NAME)
                        .param("content", "您好，我已收到您的请求，请提供一下您的账户信息。")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var detail = ticketAppService.getTicketDetail(ticketId);
        boolean hasReply = detail.getMessages().stream()
                .anyMatch(m -> "AGENT".equals(m.getSenderType())
                        && m.getContent().contains("账户信息"));
        assertTrue(hasReply, "Agent reply should be persisted as a ticket message");
    }

    // ======================== Phase 3: Transfer ========================

    @Test
    @DisplayName("IN_PROGRESS → transfer → TRANSFERRED")
    void transfer_inProgressToTransferred_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");

        mockMvc.perform(post("/api/v1/tickets/{id}/transfer", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("targetAgentId", "2")
                        .param("remark", "需要转交给李四处理")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("TRANSFERRED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("TRANSFERRED", updated.getStatus());
    }

    @Test
    @DisplayName("TRANSFERRED → agent accept → IN_PROGRESS")
    void accept_transferredToInProgress_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.TRANSFERRED,
                String.valueOf(AGENT_ID), "AGENT", "转交李四");

        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", "2")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    // ======================== Phase 4: Reject ========================

    @Test
    @DisplayName("IN_PROGRESS → reject → REJECTED")
    void reject_inProgressToRejected_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");

        mockMvc.perform(post("/api/v1/tickets/{id}/reject", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("reason", "该问题不属于我的处理范围")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("REJECTED", updated.getStatus());
        assertNull(updated.getAssignedAgentId(), "Rejected ticket should clear assigned agent");
    }

    // ======================== Phase 5: Defer ========================

    @Test
    @DisplayName("IN_PROGRESS → defer → DEFERRED")
    void defer_inProgressToDeferred_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");

        mockMvc.perform(post("/api/v1/tickets/{id}/defer", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("reason", "需要等待用户补充材料")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DEFERRED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("DEFERRED", updated.getStatus());
    }

    // ======================== Phase 6: Resolve ========================

    @Test
    @DisplayName("IN_PROGRESS → resolve → RESOLVED")
    void resolve_inProgressToResolved_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");

        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("remark", "已帮助用户重置密码，问题已解决")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("RESOLVED", updated.getStatus());
        assertNotNull(updated.getResolvedAt(), "Resolved ticket should have resolvedAt timestamp");
    }

    // ======================== Phase 7: Close ========================

    @Test
    @DisplayName("RESOLVED → close → CLOSED")
    void close_resolvedToClosed_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.RESOLVED,
                String.valueOf(AGENT_ID), "AGENT", "已解决");

        mockMvc.perform(post("/api/v1/tickets/{id}/close", ticketId)
                        .param("operatorId", String.valueOf(AGENT_ID))
                        .param("operatorType", "AGENT")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("CLOSED", updated.getStatus());
        assertNotNull(updated.getClosedAt(), "Closed ticket should have closedAt timestamp");
    }

    @Test
    @DisplayName("CLOSED is terminal → cannot transition further")
    void closed_isTerminal_cannotTransition() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.RESOLVED,
                String.valueOf(AGENT_ID), "AGENT", "已解决");
        ticketAppService.transitionStatus(ticketId, TicketStatus.CLOSED,
                String.valueOf(AGENT_ID), "AGENT", "关闭工单");

        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));

        Ticket unchanged = ticketMapper.selectById(ticketId);
        assertEquals("CLOSED", unchanged.getStatus(), "Closed ticket should not change status");
    }

    // ======================== Full Lifecycle ========================

    @Test
    @DisplayName("full lifecycle: PENDING → IN_PROGRESS → RESOLVED → CLOSED, all state logs recorded")
    void fullLifecycle_allStates_shouldRecordAllLogs() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/tickets/{id}/reply", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("agentName", AGENT_NAME)
                        .param("content", "您好，我来帮您处理。")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED));

        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .param("remark", "问题已解决")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(post("/api/v1/tickets/{id}/close", ticketId)
                        .param("operatorId", String.valueOf(AGENT_ID))
                        .param("operatorType", "AGENT")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        Ticket finalTicket = ticketMapper.selectById(ticketId);
        assertEquals("CLOSED", finalTicket.getStatus());
        assertNotNull(finalTicket.getResolvedAt());
        assertNotNull(finalTicket.getClosedAt());

        List<TicketStateLog> logs = stateLogMapper.selectList(
                new LambdaQueryWrapper<TicketStateLog>()
                        .eq(TicketStateLog::getTicketId, ticketId)
                        .orderByAsc(TicketStateLog::getCreatedAt));
        assertEquals(3, logs.size(), "Should have 3 state transition logs");

        assertEquals("PENDING", logs.get(0).getFromStatus());
        assertEquals("IN_PROGRESS", logs.get(0).getToStatus());
        assertEquals(String.valueOf(AGENT_ID), logs.get(0).getOperatorId());

        assertEquals("IN_PROGRESS", logs.get(1).getFromStatus());
        assertEquals("RESOLVED", logs.get(1).getToStatus());

        assertEquals("RESOLVED", logs.get(2).getFromStatus());
        assertEquals("CLOSED", logs.get(2).getToStatus());

        var detail = ticketAppService.getTicketDetail(ticketId);
        assertTrue(detail.getMessages().size() > 2,
                "Should have more than just the initial user message and the agent reply");
        boolean hasUserMsg = detail.getMessages().stream()
                .anyMatch(m -> "USER".equals(m.getSenderType()));
        boolean hasAgentMsg = detail.getMessages().stream()
                .anyMatch(m -> "AGENT".equals(m.getSenderType()));
        assertTrue(hasUserMsg, "Should contain user message");
        assertTrue(hasAgentMsg, "Should contain agent reply");
    }

    // ======================== GET endpoints ========================

    @Test
    @DisplayName("GET /tickets → list all tickets")
    void listTickets_shouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @DisplayName("GET /tickets/{id} → get ticket detail")
    void getTicketDetail_shouldReturnTicketWithMessages() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ticket.ticketNo").isNotEmpty())
                .andExpect(jsonPath("$.data.ticket.status").value("PENDING"))
                .andExpect(jsonPath("$.data.messages").isArray());
    }

    @Test
    @DisplayName("GET /tickets/{id} for non-existent ticket → should return error")
    void getTicketDetail_nonExistent_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{id}", 99999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001));
    }

    // ======================== Summary generation ========================

    @Test
    @DisplayName("generate summary after ticket is resolved")
    void generateSummary_shouldProduceSummary() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.agentReply(ticketId, AGENT_ID, AGENT_NAME, "已处理完成");

        String summary = ticketAppService.generateSummary(ticketId);
        assertNotNull(summary);
        assertFalse(summary.isEmpty());

        Ticket updated = ticketMapper.selectById(ticketId);
        assertNotNull(updated.getContextSummary());
    }

    // ======================== WAITING_CONFIRM transitions ========================

    @Test
    @DisplayName("RESOLVED → waiting confirm → WAITING_CONFIRM")
    void waitingConfirm_resolvedToWaitingConfirm_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.RESOLVED,
                String.valueOf(AGENT_ID), "AGENT", "已解决");

        Ticket result = ticketAppService.transitionStatus(ticketId, TicketStatus.WAITING_CONFIRM,
                "test_user_lifecycle", "USER", "用户确认已解决");

        assertEquals("WAITING_CONFIRM", result.getStatus());
        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("WAITING_CONFIRM", updated.getStatus());

        List<TicketStateLog> logs = stateLogMapper.selectList(
                new LambdaQueryWrapper<TicketStateLog>()
                        .eq(TicketStateLog::getTicketId, ticketId)
                        .orderByDesc(TicketStateLog::getCreatedAt));
        assertEquals("RESOLVED", logs.get(0).getFromStatus());
        assertEquals("WAITING_CONFIRM", logs.get(0).getToStatus());
    }

    @Test
    @DisplayName("WAITING_CONFIRM → close → CLOSED")
    void close_waitingConfirmToClosed_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.RESOLVED,
                String.valueOf(AGENT_ID), "AGENT", "已解决");
        ticketAppService.transitionStatus(ticketId, TicketStatus.WAITING_CONFIRM,
                "test_user_lifecycle", "USER", "用户确认");

        mockMvc.perform(post("/api/v1/tickets/{id}/close", ticketId)
                        .param("operatorId", "test_user_lifecycle")
                        .param("operatorType", "USER")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("CLOSED", updated.getStatus());
        assertNotNull(updated.getClosedAt(), "Closed ticket should have closedAt timestamp");
    }

    @Test
    @DisplayName("WAITING_CONFIRM → accept → IN_PROGRESS (user not satisfied)")
    void accept_waitingConfirmToInProgress_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.RESOLVED,
                String.valueOf(AGENT_ID), "AGENT", "已解决");
        ticketAppService.transitionStatus(ticketId, TicketStatus.WAITING_CONFIRM,
                "test_user_lifecycle", "USER", "用户确认");

        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    // ======================== Exit transitions from non-standard states ========================

    @Test
    @DisplayName("REJECTED → PENDING (re-queue to pending pool)")
    void rejected_toPending_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.REJECTED,
                String.valueOf(AGENT_ID), "AGENT", "不属于我的范围");

        Ticket result = ticketAppService.transitionStatus(ticketId, TicketStatus.PENDING,
                String.valueOf(AGENT_ID), "AGENT", "驳回后重新分配");

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getAssignedAgentId(), "PENDING ticket should have no assigned agent");

        List<TicketStateLog> logs = stateLogMapper.selectList(
                new LambdaQueryWrapper<TicketStateLog>()
                        .eq(TicketStateLog::getTicketId, ticketId)
                        .orderByDesc(TicketStateLog::getCreatedAt));
        assertEquals("REJECTED", logs.get(0).getFromStatus());
        assertEquals("PENDING", logs.get(0).getToStatus());
    }

    @Test
    @DisplayName("DEFERRED → accept → IN_PROGRESS (agent resumes deferred ticket)")
    void accept_deferredToInProgress_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.DEFERRED,
                String.valueOf(AGENT_ID), "AGENT", "等待用户补充材料");

        mockMvc.perform(post("/api/v1/tickets/{id}/accept", ticketId)
                        .param("agentId", String.valueOf(AGENT_ID))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Ticket updated = ticketMapper.selectById(ticketId);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    @DisplayName("TRANSFERRED → PENDING (timeout fallback re-queue)")
    void transferred_toPending_timeoutFallback_shouldSucceed() throws Exception {
        ticketAppService.transitionStatus(ticketId, TicketStatus.IN_PROGRESS,
                String.valueOf(AGENT_ID), "AGENT", "客服领取");
        ticketAppService.transitionStatus(ticketId, TicketStatus.TRANSFERRED,
                String.valueOf(AGENT_ID), "AGENT", "转交李四");

        Ticket result = ticketAppService.transitionStatus(ticketId, TicketStatus.PENDING,
                "SYSTEM", "SYSTEM", "转交超时，重新入待分配池");

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getAssignedAgentId(), "PENDING ticket should have no assigned agent");

        List<TicketStateLog> logs = stateLogMapper.selectList(
                new LambdaQueryWrapper<TicketStateLog>()
                        .eq(TicketStateLog::getTicketId, ticketId)
                        .orderByDesc(TicketStateLog::getCreatedAt));
        assertEquals("TRANSFERRED", logs.get(0).getFromStatus());
        assertEquals("PENDING", logs.get(0).getToStatus());
    }
}
