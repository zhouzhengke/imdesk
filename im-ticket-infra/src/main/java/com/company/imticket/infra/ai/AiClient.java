package com.company.imticket.infra.ai;

import com.company.imticket.infra.ai.dto.IntentRecognitionContext;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AiClient(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public RoutingResult recognizeIntent(IntentRecognitionContext context) {
        String systemPrompt = """
            你是一个IM工单系统的智能路由助手。根据用户消息和上下文，输出JSON：
            {
              "intent": "ticket_query|create_ticket|supplement|knowledge_query|business_query|chitchat|urgent_escalation",
              "confidence": 0.0-1.0,
              "entities": {"ticket_id": "IM-xxx", "keywords": ["关键词1"]},
              "priority": "normal|high|urgent",
              "sentiment": "positive|neutral|negative"
            }
            只输出JSON，不要任何解释。
            """;

        String userMessage = String.format("""
            当前上下文：
            - 资方：%s
            - 用户：%s
            - 是否有未关闭工单：%s
            - 近期KB交互轮次：%d

            用户消息：%s
            """,
            context.getCapitalName(),
            context.getUserName(),
            context.isHasOpenTicket() ? "是" : "否",
            context.getKbInteractionCount(),
            context.getMessage()
        );

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        return parseRoutingResult(response);
    }

    public String generateTicketSummary(String conversationText) {
        String prompt = "请用一句话总结以下工单对话的核心问题和解决方案（100字以内）：\n" + conversationText;
        return chatClient.prompt().user(prompt).call().content();
    }

    public String generateKnowledgeAnswer(String question, String context) {
        String systemPrompt = "你是一个客服助手。根据提供的参考信息回答用户问题。如果信息不足以回答，请明确说明。";
        String userPrompt = String.format("参考信息：\n%s\n\n用户问题：%s", context, question);
        return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
    }

    private RoutingResult parseRoutingResult(String json) {
        try {
            return objectMapper.readValue(json, RoutingResult.class);
        } catch (Exception e) {
            log.error("Failed to parse LLM routing result: {}", json, e);
            return RoutingResult.fallback();
        }
    }
}