package com.company.imticket.api.websocket;

import com.company.imticket.dao.entity.Ticket;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TicketWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public TicketWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Push new ticket notification to a specific agent.
     */
    public void pushNewTicketToAgent(Long agentId, Ticket ticket) {
        messagingTemplate.convertAndSend(
                "/topic/agent/" + agentId + "/new-ticket", ticket);
    }

    /**
     * Push ticket status change to all subscribers.
     */
    public void pushTicketStatusChange(Ticket ticket) {
        messagingTemplate.convertAndSend(
                "/topic/ticket/" + ticket.getId() + "/status",
                Map.of("ticketNo", ticket.getTicketNo(), "status", ticket.getStatus()));
    }

    /**
     * Push unassigned pool alert to duty managers.
     */
    public void pushUnassignedAlert(int pendingCount) {
        messagingTemplate.convertAndSend(
                "/topic/duty/unassigned-alert",
                Map.of("pendingCount", pendingCount, "timestamp", System.currentTimeMillis()));
    }
}
