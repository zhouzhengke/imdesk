package com.company.imticket.service.domain.ticket;

import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketStateLog;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketStateLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Domain service for ticket state machine transitions.
 * <p>
 * Transaction boundaries are owned by the Application Service layer, not this domain service.
 * The caller (Application Service) is responsible for wrapping the transition call in a
 * transaction if needed.
 */
@Service
public class TicketStateMachine {

    private static final Logger log = LoggerFactory.getLogger(TicketStateMachine.class);

    private final TicketMapper ticketMapper;
    private final TicketStateLogMapper ticketStateLogMapper;

    public TicketStateMachine(TicketMapper ticketMapper, TicketStateLogMapper ticketStateLogMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketStateLogMapper = ticketStateLogMapper;
    }

    public Ticket transition(Ticket ticket, TicketStatus target, String operatorId, String operatorType, String remark) {
        // 0. Guard against null status
        if (ticket.getStatus() == null) {
            throw new TicketException(BizErrorCode.PARAM_INVALID, "ticket.status is null");
        }
        TicketStatus fromStatus = TicketStatus.valueOf(ticket.getStatus());

        // 1. Validate transition legality
        if (!fromStatus.canTransition(target)) {
            throw new TicketException(BizErrorCode.TICKET_STATUS_ILLEGAL,
                    String.format("ticket=%s, from=%s, to=%s", ticket.getTicketNo(), fromStatus.name(), target.name()));
        }

        // 2. Validate IN_PROGRESS requires assignedAgentId
        if (target == TicketStatus.IN_PROGRESS && ticket.getAssignedAgentId() == null) {
            throw new TicketException(BizErrorCode.PARAM_INVALID,
                    "转处理中状态必须先分配客服");
        }

        // 3. Apply side effects per target state
        switch (target) {
            case PENDING:
                ticket.setAssignedAgentId(null);
                break;
            case RESOLVED:
                ticket.setResolvedAt(LocalDateTime.now());
                break;
            case CLOSED:
                ticket.setClosedAt(LocalDateTime.now());
                break;
            case REJECTED:
                ticket.setAssignedAgentId(null);
                break;
            default:
                break;
        }

        // 4. Update status
        ticket.setStatus(target.name());

        // 5. Persist ticket update
        ticketMapper.updateById(ticket);

        // 6. Record state transition log
        TicketStateLog stateLog = new TicketStateLog();
        stateLog.setTicketId(ticket.getId());
        stateLog.setFromStatus(fromStatus.name());
        stateLog.setToStatus(target.name());
        stateLog.setOperatorId(operatorId);
        stateLog.setOperatorType(operatorType);
        stateLog.setRemark(remark);
        ticketStateLogMapper.insert(stateLog);

        log.info("ticket state transition: {} {} → {}, operator={}",
                ticket.getTicketNo(), fromStatus.name(), target.name(), operatorId);

        return ticket;
    }
}