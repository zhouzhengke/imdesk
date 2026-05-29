package com.company.imticket.service.domain.ticket;

import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketStateLog;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketStateLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketStateMachineTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketStateLogMapper ticketStateLogMapper;

    @InjectMocks
    private TicketStateMachine ticketStateMachine;

    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticket = createTicket(TicketStatus.PENDING, null);
    }

    // ======================== Valid transitions ========================

    @Test
    void transition_PendingToInProgress_shouldSucceed() {
        ticket.setAssignedAgentId(100L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "领取工单");

        assertEquals("IN_PROGRESS", ticket.getStatus());
        assertEquals(100L, ticket.getAssignedAgentId());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("PENDING", "IN_PROGRESS", "agent-001", "AGENT", "领取工单");
    }

    @Test
    void transition_InProgressToResolved_shouldSucceed() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.RESOLVED, "agent-001", "AGENT", "问题已解决");

        assertEquals("RESOLVED", ticket.getStatus());
        assertNotNull(ticket.getResolvedAt());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("IN_PROGRESS", "RESOLVED", "agent-001", "AGENT", "问题已解决");
    }

    @Test
    void transition_InProgressToTransferred_shouldSucceed() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);
        ticket.setAssignedAgentId(200L);

        ticketStateMachine.transition(ticket, TicketStatus.TRANSFERRED, "agent-001", "AGENT", "转交二组");

        assertEquals("TRANSFERRED", ticket.getStatus());
        assertEquals(200L, ticket.getAssignedAgentId());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("IN_PROGRESS", "TRANSFERRED", "agent-001", "AGENT", "转交二组");
    }

    @Test
    void transition_InProgressToRejected_shouldSucceed() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.REJECTED, "agent-001", "AGENT", "不属于我的范围");

        assertEquals("REJECTED", ticket.getStatus());
        assertNull(ticket.getAssignedAgentId());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("IN_PROGRESS", "REJECTED", "agent-001", "AGENT", "不属于我的范围");
    }

    @Test
    void transition_InProgressToDeferred_shouldSucceed() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.DEFERRED, "agent-001", "AGENT", "需等待资方回复");

        assertEquals("DEFERRED", ticket.getStatus());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("IN_PROGRESS", "DEFERRED", "agent-001", "AGENT", "需等待资方回复");
    }

    @Test
    void transition_TransferredToInProgress_shouldSucceed() {
        ticket = createTicket(TicketStatus.TRANSFERRED, 200L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-003", "AGENT", "二组领取");

        assertEquals("IN_PROGRESS", ticket.getStatus());
        assertEquals(200L, ticket.getAssignedAgentId());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("TRANSFERRED", "IN_PROGRESS", "agent-003", "AGENT", "二组领取");
    }

    @Test
    void transition_TransferredToPending_shouldSucceed() {
        ticket = createTicket(TicketStatus.TRANSFERRED, 200L);

        ticketStateMachine.transition(ticket, TicketStatus.PENDING, "system", "SYSTEM", "超时退回待分配");

        assertEquals("PENDING", ticket.getStatus());
        assertNull(ticket.getAssignedAgentId());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("TRANSFERRED", "PENDING", "system", "SYSTEM", "超时退回待分配");
    }

    @Test
    void transition_RejectedToPending_shouldSucceed() {
        ticket = createTicket(TicketStatus.REJECTED, null);

        ticketStateMachine.transition(ticket, TicketStatus.PENDING, "system", "SYSTEM", "重新分配");

        assertEquals("PENDING", ticket.getStatus());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("REJECTED", "PENDING", "system", "SYSTEM", "重新分配");
    }

    @Test
    void transition_DeferredToInProgress_shouldSucceed() {
        ticket = createTicket(TicketStatus.DEFERRED, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "资方已回复，继续处理");

        assertEquals("IN_PROGRESS", ticket.getStatus());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("DEFERRED", "IN_PROGRESS", "agent-001", "AGENT", "资方已回复，继续处理");
    }

    @Test
    void transition_ResolvedToWaitingConfirm_shouldSucceed() {
        ticket = createTicket(TicketStatus.RESOLVED, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.WAITING_CONFIRM, "agent-001", "AGENT", "等待用户确认");

        assertEquals("WAITING_CONFIRM", ticket.getStatus());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("RESOLVED", "WAITING_CONFIRM", "agent-001", "AGENT", "等待用户确认");
    }

    @Test
    void transition_ResolvedToClosed_shouldSucceed() {
        ticket = createTicket(TicketStatus.RESOLVED, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.CLOSED, "agent-001", "AGENT", "直接关闭");

        assertEquals("CLOSED", ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("RESOLVED", "CLOSED", "agent-001", "AGENT", "直接关闭");
    }

    @Test
    void transition_WaitingConfirmToClosed_shouldSucceed() {
        ticket = createTicket(TicketStatus.WAITING_CONFIRM, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.CLOSED, "user-001", "CAPITAL", "用户确认关闭");

        assertEquals("CLOSED", ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("WAITING_CONFIRM", "CLOSED", "user-001", "CAPITAL", "用户确认关闭");
    }

    @Test
    void transition_WaitingConfirmToInProgress_shouldSucceed() {
        ticket = createTicket(TicketStatus.WAITING_CONFIRM, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "用户不满意，重新处理");

        assertEquals("IN_PROGRESS", ticket.getStatus());
        verify(ticketMapper).updateById(ticket);
        verifyStateLog("WAITING_CONFIRM", "IN_PROGRESS", "agent-001", "AGENT", "用户不满意，重新处理");
    }

    // ======================== Invalid transitions ========================

    @Test
    void transition_PendingToResolved_shouldThrow() {
        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.RESOLVED, "agent-001", "AGENT", "直接解决"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(ticketStateLogMapper, never()).insert(any(TicketStateLog.class));
    }

    @Test
    void transition_PendingToClosed_shouldThrow() {
        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.CLOSED, "agent-001", "AGENT", "直接关闭"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
    }

    @Test
    void transition_ClosedToAny_shouldThrow() {
        ticket = createTicket(TicketStatus.CLOSED, 100L);
        ticket.setClosedAt(LocalDateTime.now());

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "重新打开"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
    }

    @Test
    void transition_InProgressToPending_shouldThrow() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.PENDING, "system", "SYSTEM", "退回待分配"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
    }

    @Test
    void transition_ResolvedToPending_shouldThrow() {
        ticket = createTicket(TicketStatus.RESOLVED, 100L);

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.PENDING, "agent-001", "AGENT", "退回"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
    }

    @Test
    void transition_RejectedToInProgress_shouldThrow() {
        ticket = createTicket(TicketStatus.REJECTED, null);

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-002", "AGENT", "直接处理"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
    }

    @Test
    void transition_DeferredToResolved_shouldThrow() {
        ticket = createTicket(TicketStatus.DEFERRED, 100L);

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.RESOLVED, "agent-001", "AGENT", "直接解决"));

        assertEquals(BizErrorCode.TICKET_STATUS_ILLEGAL, ex.getErrorCode());
    }

    // ======================== Side effects ========================

    @Test
    void transition_ToResolved_shouldSetResolvedAt() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);
        assertNull(ticket.getResolvedAt());

        ticketStateMachine.transition(ticket, TicketStatus.RESOLVED, "agent-001", "AGENT", "解决");

        assertNotNull(ticket.getResolvedAt());
        assertNull(ticket.getClosedAt());
    }

    @Test
    void transition_ToClosed_shouldSetClosedAt() {
        ticket = createTicket(TicketStatus.RESOLVED, 100L);
        assertNull(ticket.getClosedAt());

        ticketStateMachine.transition(ticket, TicketStatus.CLOSED, "agent-001", "AGENT", "关闭");

        assertNotNull(ticket.getClosedAt());
    }

    @Test
    void transition_ToRejected_shouldClearAssignedAgent() {
        ticket = createTicket(TicketStatus.IN_PROGRESS, 100L);
        assertEquals(100L, ticket.getAssignedAgentId());

        ticketStateMachine.transition(ticket, TicketStatus.REJECTED, "agent-001", "AGENT", "驳回");

        assertNull(ticket.getAssignedAgentId());
    }

    @Test
    void transition_ToPendingFromTransferred_shouldPreserveNullAgent() {
        ticket = createTicket(TicketStatus.TRANSFERRED, null);

        ticketStateMachine.transition(ticket, TicketStatus.PENDING, "system", "SYSTEM", "超时退回");

        assertNull(ticket.getAssignedAgentId());
    }

    // ======================== Parameter validation ========================

    @Test
    void transition_ToInProgressWithoutAgentId_shouldThrowParamInvalid() {
        ticket = createTicket(TicketStatus.PENDING, null);

        TicketException ex = assertThrows(TicketException.class, () ->
                ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "system", "SYSTEM", "自动分配"));

        assertEquals(BizErrorCode.PARAM_INVALID, ex.getErrorCode());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(ticketStateLogMapper, never()).insert(any(TicketStateLog.class));
    }

    @Test
    void transition_ToInProgressWithAgentId_shouldKeepAgentId() {
        ticket = createTicket(TicketStatus.PENDING, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "system", "SYSTEM", "分配成功");

        assertEquals("IN_PROGRESS", ticket.getStatus());
        assertEquals(100L, ticket.getAssignedAgentId());
    }

    @Test
    void transition_ShouldRecordStateLogWithCorrectFields() {
        ticket = createTicket(TicketStatus.PENDING, 100L);

        ticketStateMachine.transition(ticket, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "领取工单");

        ArgumentCaptor<TicketStateLog> captor = ArgumentCaptor.forClass(TicketStateLog.class);
        verify(ticketStateLogMapper).insert(captor.capture());
        TicketStateLog log = captor.getValue();

        assertEquals(ticket.getId(), log.getTicketId());
        assertEquals("PENDING", log.getFromStatus());
        assertEquals("IN_PROGRESS", log.getToStatus());
        assertEquals("agent-001", log.getOperatorId());
        assertEquals("AGENT", log.getOperatorType());
        assertEquals("领取工单", log.getRemark());
    }

    // ======================== Null input validation ========================

    @Test
    void transition_NullTicket_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                ticketStateMachine.transition(null, TicketStatus.IN_PROGRESS, "agent-001", "AGENT", "无工单"));
    }

    @Test
    void transition_NullTargetStatus_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                ticketStateMachine.transition(ticket, null, "agent-001", "AGENT", "无目标状态"));
    }

    // ======================== Helper methods ========================

    private Ticket createTicket(TicketStatus status, Long assignedAgentId) {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNo("IM-20260526-0001");
        ticket.setStatus(status.name());
        ticket.setCapitalId(10L);
        ticket.setChannel("wecom");
        ticket.setAssignedAgentId(assignedAgentId);
        return ticket;
    }

    private void verifyStateLog(String fromStatus, String toStatus, String operatorId, String operatorType, String remark) {
        ArgumentCaptor<TicketStateLog> captor = ArgumentCaptor.forClass(TicketStateLog.class);
        verify(ticketStateLogMapper).insert(captor.capture());
        TicketStateLog log = captor.getValue();
        assertEquals(ticket.getId(), log.getTicketId());
        assertEquals(fromStatus, log.getFromStatus());
        assertEquals(toStatus, log.getToStatus());
        assertEquals(operatorId, log.getOperatorId());
        assertEquals(operatorType, log.getOperatorType());
        assertEquals(remark, log.getRemark());
    }
}