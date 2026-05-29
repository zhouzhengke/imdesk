package com.company.imticket.service.application.ticket;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketMessage;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketMessageMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.infra.notification.NotificationSender;
import com.company.imticket.service.domain.duty.AssignmentResult;
import com.company.imticket.service.domain.duty.DutyAssignmentService;
import com.company.imticket.service.domain.ticket.TicketNoGenerator;
import com.company.imticket.service.domain.ticket.TicketStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service for ticket lifecycle management.
 * <p>
 * Orchestrates ticket creation, status transitions, message management, agent assignment,
 * and notification dispatch. Delegates domain logic to {@link TicketStateMachine} and
 * {@link DutyAssignmentService}.
 */
@Service
public class TicketAppService {

    private static final Logger log = LoggerFactory.getLogger(TicketAppService.class);

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper messageMapper;
    private final TicketNoGenerator noGenerator;
    private final TicketStateMachine stateMachine;
    private final DutyAssignmentService dutyAssignment;
    private final NotificationSender notificationSender;
    private final AiClient aiClient;
    private final SessionCacheService sessionCache;

    public TicketAppService(TicketMapper ticketMapper,
                           TicketMessageMapper messageMapper,
                           TicketNoGenerator noGenerator,
                           TicketStateMachine stateMachine,
                           DutyAssignmentService dutyAssignment,
                           NotificationSender notificationSender,
                           AiClient aiClient,
                           SessionCacheService sessionCache) {
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
        this.noGenerator = noGenerator;
        this.stateMachine = stateMachine;
        this.dutyAssignment = dutyAssignment;
        this.notificationSender = notificationSender;
        this.aiClient = aiClient;
        this.sessionCache = sessionCache;
    }

    /**
     * Create a new ticket from a channel message.
     * <p>
     * Generates a ticket number, persists the ticket and first message,
     * assigns a duty agent, and sends notifications to both the user and the agent.
     * If the user already has an open ticket, appends the message to it instead.
     */
    @Transactional
    public Ticket createTicket(ChannelMessage msg, Long capitalId, String capitalName,
                               String priority, String description) {
        Ticket existing = ticketMapper.findOpenTicketByChannelUserForUpdate(
                msg.getChannel(), msg.getChannelUserId());
        if (existing != null) {
            appendMessage(existing, msg);
            log.info("user already has open ticket: ticketNo={}, user={}, channel={}",
                    existing.getTicketNo(), msg.getChannelUserId(), msg.getChannel());
            throw new TicketException(BizErrorCode.TICKET_HAS_OPEN,
                    "已有工单 " + existing.getTicketNo() + " 处理中，消息已追加");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(noGenerator.generate());
        ticket.setCapitalId(capitalId);
        ticket.setChannel(msg.getChannel());
        ticket.setChannelUserId(msg.getChannelUserId());
        ticket.setChannelGroupId(msg.getChannelGroupId());
        ticket.setUserName(msg.getUserName());
        ticket.setDescription(description);
        ticket.setPriority(priority != null ? priority : "normal");
        ticketMapper.insert(ticket);

        TicketMessage firstMsg = new TicketMessage();
        firstMsg.setTicketId(ticket.getId());
        firstMsg.setSenderType("USER");
        firstMsg.setSenderId(msg.getChannelUserId());
        firstMsg.setSenderName(msg.getUserName());
        firstMsg.setContent(msg.getContent());
        firstMsg.setContentType(msg.getContentType());
        messageMapper.insert(firstMsg);

        AssignmentResult assign = dutyAssignment.assign(
                msg.getChannel(), msg.getChannelUserId(), msg.getChannelGroupId(), capitalName);
        if (assign != null) {
            ticket.setAssignedAgentId(assign.getAgentId());
            ticketMapper.updateById(ticket);
            sessionCache.cacheLastAgent(msg.getChannel(), msg.getChannelUserId(),
                    msg.getChannelGroupId(), assign.getAgentId());
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("ticket_no", ticket.getTicketNo());
        vars.put("capital_name", capitalName);
        vars.put("user_name", msg.getUserName() != null ? msg.getUserName() : "");
        vars.put("description", description != null ? description : msg.getContent());

        notificationSender.sendByTemplate("ticket_created_to_user", vars,
                msg.getChannelUserId(), msg.getChannelGroupId());

        if (assign != null) {
            vars.put("agent_name", assign.getAgentName());
            notificationSender.sendByTemplate("ticket_assigned_to_agent", vars,
                    String.valueOf(assign.getAgentId()), null);
        }

        log.info("ticket created: ticketNo={}, capital={}, user={}, agent={}, priority={}",
                ticket.getTicketNo(), capitalName, msg.getUserName(),
                assign != null ? assign.getAgentName() : "unassigned", priority);
        return ticket;
    }

    /**
     * Transition a ticket to a new status via the state machine.
     */
    @Transactional
    public Ticket transitionStatus(Long ticketId, TicketStatus target,
                                    String operatorId, String operatorType, String remark) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        return stateMachine.transition(ticket, target, operatorId, operatorType, remark);
    }

    /**
     * List tickets with filters and pagination.
     */
    public PageResp<Ticket> listTickets(int page, int size, String channel,
                                         Long capitalId, String status, Long agentId, String keyword) {
        Page<Ticket> p = new Page<>(page, size);
        IPage<Ticket> result = ticketMapper.selectPageWithFilters(p, channel, capitalId, status, agentId, keyword);
        PageResp<Ticket> resp = new PageResp<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }

    /**
     * Get ticket detail with full message history.
     */
    public TicketDetail getTicketDetail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        List<TicketMessage> messages = messageMapper.selectByTicketId(ticketId);
        TicketDetail detail = new TicketDetail();
        detail.setTicket(ticket);
        detail.setMessages(messages);
        return detail;
    }

    /**
     * Append a user message to an existing ticket.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendMessage(Ticket ticket, ChannelMessage msg) {
        TicketMessage tm = new TicketMessage();
        tm.setTicketId(ticket.getId());
        tm.setSenderType("USER");
        tm.setSenderId(msg.getChannelUserId());
        tm.setSenderName(msg.getUserName());
        tm.setContent(msg.getContent());
        tm.setContentType(msg.getContentType());
        messageMapper.insert(tm);
    }

    /**
     * Agent sends a reply to a ticket.
     */
    public void agentReply(Long ticketId, Long agentId, String agentName, String content) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        TicketMessage msg = new TicketMessage();
        msg.setTicketId(ticketId);
        msg.setSenderType("AGENT");
        msg.setSenderId(String.valueOf(agentId));
        msg.setSenderName(agentName);
        msg.setContent(content);
        msg.setContentType("text");
        messageMapper.insert(msg);
        log.info("agent reply: ticketNo={}, agentId={}, agentName={}", ticket.getTicketNo(), agentId, agentName);
    }

    /**
     * Generate and persist a ticket summary using LLM.
     */
    public String generateSummary(Long ticketId) {
        TicketDetail detail = getTicketDetail(ticketId);
        StringBuilder conversation = new StringBuilder();
        for (TicketMessage msg : detail.getMessages()) {
            conversation.append("[").append(msg.getSenderType()).append("] ")
                    .append(msg.getSenderName()).append(": ")
                    .append(msg.getContent()).append("\n");
        }
        String summary = aiClient.generateTicketSummary(conversation.toString());
        Ticket ticket = detail.getTicket();
        ticket.setContextSummary(summary);
        ticketMapper.updateById(ticket);
        log.info("ticket summary generated: ticketNo={}", ticket.getTicketNo());
        return summary;
    }

    /**
     * Count pending (unassigned) tickets.
     */
    public long countPendingTickets() {
        return ticketMapper.countPendingTickets();
    }

    /**
     * Check if a channel user has an open ticket.
     */
    public boolean hasOpenTicket(String channel, String channelUserId) {
        return ticketMapper.findOpenTicketByChannelUser(channel, channelUserId) != null;
    }
}
