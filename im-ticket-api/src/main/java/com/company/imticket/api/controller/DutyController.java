package com.company.imticket.api.controller;

import com.company.imticket.common.dto.R;
import com.company.imticket.service.application.ticket.TicketAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/duty")
public class DutyController {

    private final TicketAppService ticketAppService;

    public DutyController(TicketAppService ticketAppService) {
        this.ticketAppService = ticketAppService;
    }

    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        return R.ok(ticketAppService.countPendingTickets());
    }

    @GetMapping("/alerts")
    public R<List<Map<String, String>>> alerts() {
        return R.ok(Collections.emptyList());
    }
}
