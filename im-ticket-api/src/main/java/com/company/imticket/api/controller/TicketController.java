package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.service.application.ticket.TicketAppService;
import com.company.imticket.service.application.ticket.TicketDetail;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@Validated
public class TicketController {

    private final TicketAppService ticketAppService;

    public TicketController(TicketAppService ticketAppService) {
        this.ticketAppService = ticketAppService;
    }

    @GetMapping
    public R<PageResp<Ticket>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                     @RequestParam(required = false) String channel,
                                     @RequestParam(required = false) Long capitalId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long agentId,
                                     @RequestParam(required = false) String keyword) {
        return R.ok(ticketAppService.listTickets(page, size, channel, capitalId, status, agentId, keyword));
    }

    @GetMapping("/{id}")
    public R<TicketDetail> detail(@PathVariable Long id) {
        return R.ok(ticketAppService.getTicketDetail(id));
    }

    // TODO: Replace @RequestParam agentId with CurrentAgent.getAgentId() once auth filter is wired
    @PostMapping("/{id}/accept")
    public R<Ticket> accept(@PathVariable Long id, @RequestParam Long agentId) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.IN_PROGRESS,
                String.valueOf(agentId), "AGENT", "客服领取"));
    }

    @PostMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @RequestParam Long agentId,
                          @RequestParam String agentName, @RequestParam String content) {
        ticketAppService.agentReply(id, agentId, agentName, content);
        return R.ok(null);
    }

    @PostMapping("/{id}/transfer")
    public R<Ticket> transfer(@PathVariable Long id, @RequestParam Long agentId,
                               @RequestParam Long targetAgentId, @RequestParam(required = false) String remark) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.TRANSFERRED,
                String.valueOf(agentId), "AGENT",
                "转交给客服" + targetAgentId + (remark != null ? ": " + remark : "")));
    }

    @PostMapping("/{id}/reject")
    public R<Ticket> reject(@PathVariable Long id, @RequestParam Long agentId,
                             @RequestParam String reason) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.REJECTED,
                String.valueOf(agentId), "AGENT", reason));
    }

    @PostMapping("/{id}/defer")
    public R<Ticket> defer(@PathVariable Long id, @RequestParam Long agentId,
                            @RequestParam String reason) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.DEFERRED,
                String.valueOf(agentId), "AGENT", reason));
    }

    @PostMapping("/{id}/resolve")
    public R<Ticket> resolve(@PathVariable Long id, @RequestParam Long agentId,
                              @RequestParam(required = false) String remark) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.RESOLVED,
                String.valueOf(agentId), "AGENT", remark));
    }

    @PostMapping("/{id}/close")
    public R<Ticket> close(@PathVariable Long id, @RequestParam String operatorId,
                            @RequestParam String operatorType) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.CLOSED,
                operatorId, operatorType, "关闭工单"));
    }
}
