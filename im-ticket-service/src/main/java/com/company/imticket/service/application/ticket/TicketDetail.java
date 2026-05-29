package com.company.imticket.service.application.ticket;

import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketMessage;

import java.util.List;

/**
 * Value object representing a ticket with its full message history.
 */
public class TicketDetail {
    private Ticket ticket;
    private List<TicketMessage> messages;

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public List<TicketMessage> getMessages() { return messages; }
    public void setMessages(List<TicketMessage> messages) { this.messages = messages; }
}
