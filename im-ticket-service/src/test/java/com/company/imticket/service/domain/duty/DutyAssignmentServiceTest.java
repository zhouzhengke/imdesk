package com.company.imticket.service.domain.duty;

import com.company.imticket.dao.entity.Agent;
import com.company.imticket.dao.mapper.AgentMapper;
import com.company.imticket.dao.mapper.ShiftScheduleMapper;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.notification.NotificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DutyAssignmentServiceTest {

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private ShiftScheduleMapper scheduleMapper;

    // Concrete class dependencies cannot use @Mock on Java 26 due to ByteBuddy limitation.
    // Use manual stubs instead.
    private SessionCacheService sessionCache;
    private NotificationSender notificationSender;
    private DutyAssignmentService dutyAssignmentService;

    // Controllable return values for stubs
    private Long lastAgentReturn;
    private String templateCodeCapture;
    private Map<String, String> variablesCapture;
    private String targetUserIdCapture;
    private String targetGroupIdCapture;

    private static final String CHANNEL = "wecom";
    private static final String CHANNEL_USER_ID = "user-001";
    private static final String CHANNEL_GROUP_ID = "group-001";
    private static final String CAPITAL_NAME = "XX银行";

    @BeforeEach
    void setUp() {
        // Reset controllable fields
        lastAgentReturn = null;
        templateCodeCapture = null;
        variablesCapture = null;
        targetUserIdCapture = null;
        targetGroupIdCapture = null;

        // Stub SessionCacheService — only getLastAgent is called by DutyAssignmentService
        sessionCache = new SessionCacheService(null) {
            @Override
            public Long getLastAgent(String channel, String userId, String groupId) {
                return lastAgentReturn;
            }
        };

        // Stub NotificationSender — only sendByTemplate is called by DutyAssignmentService
        notificationSender = new NotificationSender(null, null, null) {
            @Override
            public void sendByTemplate(String templateCode, Map<String, String> variables,
                                        String targetUserId, String targetGroupId) {
                templateCodeCapture = templateCode;
                variablesCapture = variables;
                targetUserIdCapture = targetUserId;
                targetGroupIdCapture = targetGroupId;
            }
        };

        dutyAssignmentService = new DutyAssignmentService(
                agentMapper, scheduleMapper, sessionCache, notificationSender, new ObjectMapper());
    }

    // ======================== Context inheritance tests ========================

    @Test
    void assign_contextInheritance_agentOnline_shouldReturnContextInheritance() {
        Agent agent = createAgent(1L, "张三", "ONLINE");
        lastAgentReturn = 1L;
        when(agentMapper.selectById(1L)).thenReturn(agent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(1L, result.getAgentId());
        assertEquals("张三", result.getAgentName());
        assertEquals("context_inheritance", result.getStrategy());

        verify(agentMapper).selectById(1L);
        verifyNoInteractions(scheduleMapper);
        assertNull(templateCodeCapture, "notification should not be sent");
    }

    @Test
    void assign_contextInheritance_agentOffline_shouldFallToPrimary() {
        Agent agent = createAgent(1L, "张三", "OFFLINE");
        Agent dutyAgent = createAgent(10L, "李四", "ONLINE");
        lastAgentReturn = 1L;
        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(agentMapper.selectById(10L)).thenReturn(dutyAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(10L, result.getAgentId());
        assertEquals("李四", result.getAgentName());
        assertEquals("primary_duty", result.getStrategy());

        verify(agentMapper).selectById(1L);
        verify(agentMapper).selectById(10L);
        verify(scheduleMapper).findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class));
        assertNull(templateCodeCapture, "notification should not be sent");
    }

    @Test
    void assign_contextInheritance_noLastAgent_shouldFallToPrimary() {
        Agent dutyAgent = createAgent(10L, "李四", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(agentMapper.selectById(10L)).thenReturn(dutyAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(10L, result.getAgentId());
        assertEquals("primary_duty", result.getStrategy());

        verify(agentMapper, never()).selectById(1L);
        verify(agentMapper).selectById(10L);
    }

    @Test
    void assign_contextInheritance_agentNotFound_shouldFallToPrimary() {
        Agent dutyAgent = createAgent(10L, "李四", "ONLINE");
        lastAgentReturn = 999L;
        when(agentMapper.selectById(999L)).thenReturn(null);
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(agentMapper.selectById(10L)).thenReturn(dutyAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(10L, result.getAgentId());
        assertEquals("primary_duty", result.getStrategy());
    }

    // ======================== Primary duty tests ========================

    @Test
    void assign_primaryDuty_firstOnline_shouldReturnPrimaryDuty() {
        Agent dutyAgent1 = createAgent(10L, "李四", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10,20]");
        when(agentMapper.selectById(10L)).thenReturn(dutyAgent1);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(10L, result.getAgentId());
        assertEquals("李四", result.getAgentName());
        assertEquals("primary_duty", result.getStrategy());

        verify(agentMapper).selectById(10L);
        verify(agentMapper, never()).selectById(20L);
    }

    @Test
    void assign_primaryDuty_skipOffline_shouldPickNextOnline() {
        Agent offlineAgent = createAgent(10L, "李四", "OFFLINE");
        Agent onlineAgent = createAgent(20L, "王五", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10,20]");
        when(agentMapper.selectById(10L)).thenReturn(offlineAgent);
        when(agentMapper.selectById(20L)).thenReturn(onlineAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(20L, result.getAgentId());
        assertEquals("王五", result.getAgentName());
        assertEquals("primary_duty", result.getStrategy());
    }

    @Test
    void assign_primaryDuty_allOffline_shouldFallToBackup() {
        Agent offlineAgent = createAgent(10L, "李四", "OFFLINE");
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(10L)).thenReturn(offlineAgent);
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("backup_duty", result.getStrategy());

        verify(scheduleMapper).findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class));
        verify(scheduleMapper).findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void assign_primaryDuty_noDutyAgents_shouldFallToBackup() {
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("backup_duty", result.getStrategy());
    }

    // ======================== Backup duty tests ========================

    @Test
    void assign_backupDuty_firstOnline_shouldReturnBackupDuty() {
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("赵六", result.getAgentName());
        assertEquals("backup_duty", result.getStrategy());
    }

    @Test
    void assign_backupDuty_allOffline_shouldReturnNull() {
        Agent offlineBackup = createAgent(30L, "赵六", "OFFLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(30L)).thenReturn(offlineBackup);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNull(result);
        assertEquals("duty_no_agent_available", templateCodeCapture, "alert should be sent");
    }

    // ======================== No agent available tests ========================

    @Test
    void assign_noAgentAvailable_shouldReturnNullAndSendAlert() {
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNull(result);
        assertEquals("duty_no_agent_available", templateCodeCapture);
        assertNotNull(variablesCapture);
        assertEquals(CHANNEL, variablesCapture.get("channel"));
        assertEquals(CAPITAL_NAME, variablesCapture.get("capitalName"));
    }

    @Test
    void assign_allAgentsOffline_shouldReturnNullAndSendAlert() {
        Agent offlineAgent = createAgent(10L, "李四", "OFFLINE");
        Agent offlineBackup = createAgent(30L, "赵六", "OFFLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(10L)).thenReturn(offlineAgent);
        when(agentMapper.selectById(30L)).thenReturn(offlineBackup);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNull(result);
        assertEquals("duty_no_agent_available", templateCodeCapture);
    }

    // ======================== Empty JSON array tests ========================

    @Test
    void assign_emptyJsonArray_duty_shouldFallToBackup() {
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[]");
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("backup_duty", result.getStrategy());
    }

    // ======================== Malformed JSON tests ========================

    @Test
    void assign_malformedJson_duty_shouldFallToBackup() {
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("{invalid json}");
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("backup_duty", result.getStrategy());
    }

    // ======================== Non-standard agent status tests (M9) ========================

    @Test
    void assign_primaryDuty_agentBusy_shouldSkipAndFallToBackup() {
        Agent busyAgent = createAgent(10L, "李四", "BUSY");
        Agent backupAgent = createAgent(30L, "赵六", "ONLINE");
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[10]");
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn("[30]");
        when(agentMapper.selectById(10L)).thenReturn(busyAgent);
        when(agentMapper.selectById(30L)).thenReturn(backupAgent);

        AssignmentResult result = dutyAssignmentService.assign(CHANNEL, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNotNull(result);
        assertEquals(30L, result.getAgentId());
        assertEquals("赵六", result.getAgentName());
        assertEquals("backup_duty", result.getStrategy());
    }

    // ======================== Null parameter tests (M10) ========================

    @Test
    void assign_nullChannel_shouldReturnNullOrHandleGracefully() {
        lastAgentReturn = null;
        when(scheduleMapper.findCurrentDutyAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);
        when(scheduleMapper.findCurrentBackupAgents(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(null);

        AssignmentResult result = dutyAssignmentService.assign(null, CHANNEL_USER_ID, CHANNEL_GROUP_ID, CAPITAL_NAME);

        assertNull(result);
        assertEquals("duty_no_agent_available", templateCodeCapture);
    }

    // ======================== Helper methods ========================

    private Agent createAgent(Long id, String name, String status) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setStatus(status);
        return agent;
    }
}
