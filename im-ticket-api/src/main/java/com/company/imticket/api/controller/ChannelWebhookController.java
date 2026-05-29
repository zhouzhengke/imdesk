package com.company.imticket.api.controller;

import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.ChannelException;
import com.company.imticket.infra.channel.CapitalIdentity;
import com.company.imticket.infra.channel.ChannelAdapter;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.service.application.knowledge.KnowledgeAnswerResult;
import com.company.imticket.service.application.knowledge.KnowledgeAppService;
import com.company.imticket.service.application.routing.RouteDecision;
import com.company.imticket.service.application.routing.RoutingAppService;
import com.company.imticket.service.application.ticket.TicketAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
public class ChannelWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ChannelWebhookController.class);

    private final List<ChannelAdapter> channelAdapters;
    private final RoutingAppService routingAppService;
    private final TicketAppService ticketAppService;
    private final KnowledgeAppService knowledgeAppService;
    private final ObjectMapper objectMapper;

    public ChannelWebhookController(List<ChannelAdapter> channelAdapters,
                                     RoutingAppService routingAppService,
                                     TicketAppService ticketAppService,
                                     KnowledgeAppService knowledgeAppService,
                                     ObjectMapper objectMapper) {
        this.channelAdapters = channelAdapters;
        this.routingAppService = routingAppService;
        this.ticketAppService = ticketAppService;
        this.knowledgeAppService = knowledgeAppService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/wecom")
    public String wecomCallback(@RequestParam(required = false) String signature,
                                 @RequestParam(required = false) String timestamp,
                                 @RequestParam(required = false) String nonce,
                                 @RequestBody String body) {
        ChannelAdapter adapter = findAdapter("wecom");
        Map<String, Object> payload = parseBody(body);

        if (!adapter.verifySignature(signature, timestamp, nonce, body)) {
            log.error("WeCom signature verification failed");
            return "fail";
        }

        return processMessage(adapter, payload);
    }

    @PostMapping("/feishu")
    public Map<String, Object> feishuCallback(@RequestBody Map<String, Object> body) {
        ChannelAdapter adapter = findAdapter("feishu");

        // Feishu URL verification challenge — no signature, just echo the challenge token
        if ("url_verification".equals(body.get("type"))) {
            return Map.of("challenge", body.get("challenge"));
        }

        if (!adapter.verifySignature(null, null, null, body.toString())) {
            log.error("Feishu signature verification failed");
            return Map.of("code", 403, "message", "signature verification failed");
        }

        processMessage(adapter, body);
        return Map.of("code", 0);
    }

    private String processMessage(ChannelAdapter adapter, Map<String, Object> payload) {
        ChannelMessage msg = adapter.normalize(payload);
        CapitalIdentity identity = adapter.resolveCapital(payload);

        String capitalName = identity != null ? identity.getCapitalName() : "未知资方";
        Long capitalId = identity != null ? identity.getCapitalId() : null;

        boolean hasOpenTicket = ticketAppService.hasOpenTicket(msg.getChannel(), msg.getChannelUserId());
        RouteDecision decision = routingAppService.route(msg, capitalName, hasOpenTicket);

        switch (decision.getIntent()) {
            case TICKET_QUERY:
                adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text",
                        "工单查询功能开发中，请稍后重试。工单号: " + decision.getExtractedParam());
                break;

            case KNOWLEDGE_QUERY:
            case CHITCHAT:
                KnowledgeAnswerResult kbResult = knowledgeAppService.answer(
                        msg.getChannel(), msg.getChannelUserId(), msg.getChannelGroupId(), msg.getContent());
                if (kbResult.isShouldEscalate()) {
                    ticketAppService.createTicket(msg, capitalId, capitalName, decision.getPriority(), msg.getContent());
                    adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text",
                            "已连续多次未匹配到答案，已为您创建工单，客服将尽快处理。");
                } else if (kbResult.isAnswered()) {
                    adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text", kbResult.getAnswer());
                } else {
                    adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text",
                            "暂未找到相关答案，如需帮助请描述具体问题，我们将为您创建工单。");
                }
                break;

            case CREATE_TICKET:
            case URGENT_ESCALATION:
            case SUPPLEMENT:
            default:
                ticketAppService.createTicket(msg, capitalId, capitalName, decision.getPriority(), msg.getContent());
                adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text",
                        "您的问题已创建工单，我们将尽快处理。");
                break;
        }

        return "success";
    }

    private ChannelAdapter findAdapter(String channel) {
        return channelAdapters.stream()
                .filter(a -> a.getType().getCode().equals(channel))
                .findFirst()
                .orElseThrow(() -> new ChannelException(BizErrorCode.CHANNEL_UNSUPPORTED, channel));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook body", e);
            return Map.of();
        }
    }
}
