package com.company.imticket.service.domain.duty;

import com.company.imticket.dao.entity.Agent;
import com.company.imticket.dao.mapper.AgentMapper;
import com.company.imticket.dao.mapper.ShiftScheduleMapper;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.notification.NotificationSender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service for duty-based agent assignment.
 * <p>
 * Implements a multi-level fallback assignment strategy:
 * <ol>
 *   <li>Context inheritance — re-assign to the last agent who served this session</li>
 *   <li>Primary duty — assign to the first online primary duty agent</li>
 *   <li>Backup duty — assign to the first online backup duty agent</li>
 *   <li>No agent — return null (caller handles unassigned pool + alert)</li>
 * </ol>
 */
@Service
public class DutyAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DutyAssignmentService.class);
    private static final String STRATEGY_CONTEXT_INHERITANCE = "context_inheritance";
    private static final String STRATEGY_PRIMARY_DUTY = "primary_duty";
    private static final String STRATEGY_BACKUP_DUTY = "backup_duty";
    private static final String ONLINE_STATUS = "ONLINE";
    private static final String TEMPLATE_NO_AGENT = "duty_no_agent_available";

    private final AgentMapper agentMapper;
    private final ShiftScheduleMapper scheduleMapper;
    private final SessionCacheService sessionCache;
    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    public DutyAssignmentService(AgentMapper agentMapper,
                                  ShiftScheduleMapper scheduleMapper,
                                  SessionCacheService sessionCache,
                                  NotificationSender notificationSender,
                                  ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.scheduleMapper = scheduleMapper;
        this.sessionCache = sessionCache;
        this.notificationSender = notificationSender;
        this.objectMapper = objectMapper;
    }

    /**
     * Assign an agent for the given session using the multi-level fallback strategy.
     *
     * @param channel         IM channel identifier (e.g. "wecom", "feishu")
     * @param channelUserId   user ID from the IM channel
     * @param channelGroupId  group/chat ID from the IM channel
     * @param capitalName     name of the capital/customer
     * @return an {@link AssignmentResult} with agentId, agentName, and strategy,
     *         or {@code null} if no agent is available
     */
    public AssignmentResult assign(String channel, String channelUserId,
                                    String channelGroupId, String capitalName) {
        // Level 1: Context inheritance
        AssignmentResult result = tryContextInheritance(channel, channelUserId, channelGroupId);
        if (result != null) {
            log.info("agent assigned via context_inheritance: agentId={}, agentName={}, channel={}",
                    result.getAgentId(), result.getAgentName(), channel);
            return result;
        }

        // Level 2: Primary duty
        result = tryDutyAgents(LocalDate.now(), LocalTime.now(), true);
        if (result != null) {
            log.info("agent assigned via primary_duty: agentId={}, agentName={}, channel={}",
                    result.getAgentId(), result.getAgentName(), channel);
            return result;
        }

        // Level 3: Backup duty
        result = tryDutyAgents(LocalDate.now(), LocalTime.now(), false);
        if (result != null) {
            log.info("agent assigned via backup_duty: agentId={}, agentName={}, channel={}",
                    result.getAgentId(), result.getAgentName(), channel);
            return result;
        }

        // Level 4: No agent available
        log.warn("no agent available: channel={}, capitalName={}", channel, capitalName);
        Map<String, String> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("capitalName", capitalName);
        notificationSender.sendByTemplate(TEMPLATE_NO_AGENT, variables, null, null);
        return null;
    }

    /**
     * Try to assign to the last agent who served this session (context inheritance).
     */
    private AssignmentResult tryContextInheritance(String channel, String channelUserId, String channelGroupId) {
        Long lastAgentId = sessionCache.getLastAgent(channel, channelUserId, channelGroupId);
        if (lastAgentId == null) {
            log.debug("context_inheritance: no last agent for channel={}, userId={}, groupId={}",
                    channel, channelUserId, channelGroupId);
            return null;
        }

        Agent agent = agentMapper.selectById(lastAgentId);
        if (agent == null) {
            log.debug("context_inheritance: agent not found, agentId={}", lastAgentId);
            return null;
        }

        if (!ONLINE_STATUS.equals(agent.getStatus())) {
            log.debug("context_inheritance: agent offline, agentId={}, status={}, falling to primary duty",
                    lastAgentId, agent.getStatus());
            return null;
        }

        return new AssignmentResult(agent.getId(), agent.getName(), STRATEGY_CONTEXT_INHERITANCE);
    }

    /**
     * Try to assign to the first online agent from the current duty or backup shift.
     *
     * @param date     the schedule date
     * @param time     the current time
     * @param isPrimary {@code true} for primary duty, {@code false} for backup duty
     */
    private AssignmentResult tryDutyAgents(LocalDate date, LocalTime time, boolean isPrimary) {
        String agentIdsJson;
        if (isPrimary) {
            agentIdsJson = scheduleMapper.findCurrentDutyAgents(date, time);
        } else {
            agentIdsJson = scheduleMapper.findCurrentBackupAgents(date, time);
        }

        if (agentIdsJson == null || agentIdsJson.isEmpty()) {
            log.debug("{}: no agents found for date={}, time={}",
                    isPrimary ? "primary_duty" : "backup_duty", date, time);
            return null;
        }

        List<Long> agentIds = parseAgentIds(agentIdsJson);
        if (agentIds.isEmpty()) {
            log.debug("{}: empty agent list after parsing, json={}",
                    isPrimary ? "primary_duty" : "backup_duty", agentIdsJson);
            return null;
        }

        for (Long agentId : agentIds) {
            Agent agent = agentMapper.selectById(agentId);
            if (agent != null && ONLINE_STATUS.equals(agent.getStatus())) {
                String strategy = isPrimary ? STRATEGY_PRIMARY_DUTY : STRATEGY_BACKUP_DUTY;
                return new AssignmentResult(agent.getId(), agent.getName(), strategy);
            }
            log.debug("{}: agent not available, agentId={}, status={}",
                    isPrimary ? "primary_duty" : "backup_duty",
                    agentId, agent != null ? agent.getStatus() : "NOT_FOUND");
        }

        log.debug("{}: all agents offline, agentIds={}",
                isPrimary ? "primary_duty" : "backup_duty", agentIds);
        return null;
    }

    /**
     * Parse a JSON array string like "[1,2,3]" into a List of Long.
     */
    private List<Long> parseAgentIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (IOException e) {
            log.error("failed to parse agent IDs JSON: {}", json, e);
            return Collections.emptyList();
        }
    }
}
