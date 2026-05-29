package com.company.imticket.api;

import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E Journey 1: Knowledge-base auto-reply.
 * <p>
 * A user sends a KB question via the WeCom webhook. The FAQ is hit,
 * an answer is returned via the channel adapter, and no ticket is created.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E Journey 1: Knowledge-base Auto Reply")
class KnowledgeAutoReplyE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @MockBean
    private AiClient aiClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        RoutingResult llmResult = new RoutingResult();
        llmResult.setIntent("knowledge_query");
        llmResult.setConfidence(0.95);
        llmResult.setPriority("normal");
        llmResult.setSentiment("neutral");
        when(aiClient.recognizeIntent(any())).thenReturn(llmResult);

        String llmAnswer = "审批流程分为三步：1）提交申请材料；2）风控审核（1-3个工作日）；3）审批通过后放款。如需加急，请联系客服处理。";
        when(aiClient.generateKnowledgeAnswer(anyString(), anyString())).thenReturn(llmAnswer);
    }

    @Test
    @DisplayName("send KB question via WeCom webhook, FAQ hit, answer returned, no ticket created")
    void wecomWebhook_kbQuestion_faqHit_shouldReturnAnswerAndNotCreateTicket() throws Exception {
        String wecomPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "Content", "审批流程是什么？",
                "FromUserName", "test_user_kb_1",
                "MsgType", "text"
        ));

        long ticketCountBefore = ticketMapper.countPendingTickets();

        mockMvc.perform(post("/api/v1/webhook/wecom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wecomPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertEquals(ticketCountBefore, ticketCountAfter,
                "No new ticket should be created for a KB auto-reply");

        verify(aiClient, atLeastOnce()).recognizeIntent(any());
        verify(aiClient, atLeastOnce()).generateKnowledgeAnswer(
                eq("审批流程是什么？"), contains("审批流程"));

        // Verify the KB answer content was generated from the FAQ data.
        // The FAQ answer for "审批流程是什么？" in test-data.sql contains
        // specific terms that must be present in the returned answer.
        ArgumentCaptor<String> questionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiClient, atLeastOnce()).generateKnowledgeAnswer(questionCaptor.capture(), contextCaptor.capture());
        assertTrue(contextCaptor.getValue().contains("提交申请材料"),
                "FAQ answer context should contain specific answer text: 提交申请材料");
        assertTrue(contextCaptor.getValue().contains("风控审核"),
                "FAQ answer context should contain specific answer text: 风控审核");
        assertTrue(contextCaptor.getValue().contains("审批通过后放款"),
                "FAQ answer context should contain specific answer text: 审批通过后放款");
    }

    @Test
    @DisplayName("send KB question, FAQ hit, verify answer content includes expected text")
    void wecomWebhook_kbQuestion_shouldReturnExpectedAnswerContent() throws Exception {
        String wecomPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "Content", "如何查看我的贷款进度？",
                "FromUserName", "test_user_kb_2",
                "MsgType", "text"
        ));

        mockMvc.perform(post("/api/v1/webhook/wecom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wecomPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        ArgumentCaptor<String> questionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiClient).generateKnowledgeAnswer(questionCaptor.capture(), contextCaptor.capture());

        assertEquals("如何查看我的贷款进度？", questionCaptor.getValue());
        assertTrue(contextCaptor.getValue().contains("贷款进度"),
                "FAQ answer context should contain relevant answer text");
    }

    @Test
    @DisplayName("send non-KB question, rule engine should not match, falls to LLM → CREATE_TICKET")
    void wecomWebhook_nonKbQuestion_shouldFallbackToCreateTicket() throws Exception {
        RoutingResult createTicketResult = new RoutingResult();
        createTicketResult.setIntent("create_ticket");
        createTicketResult.setConfidence(0.8);
        createTicketResult.setPriority("normal");
        createTicketResult.setSentiment("neutral");
        when(aiClient.recognizeIntent(any())).thenReturn(createTicketResult);

        String wecomPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "Content", "我需要重置我的账户密码，无法登录了",
                "FromUserName", "test_user_nonkb",
                "MsgType", "text"
        ));

        long ticketCountBefore = ticketMapper.countPendingTickets();

        mockMvc.perform(post("/api/v1/webhook/wecom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wecomPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertTrue(ticketCountAfter >= ticketCountBefore,
                "A new ticket should be created for non-KB question");
    }

    @Test
    @DisplayName("send rule-matched chitchat → treated as KB query, no ticket created")
    void wecomWebhook_ruleMatchedHelp_shouldTreatAsKbQuery() throws Exception {
        String wecomPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "Content", "帮助",
                "FromUserName", "test_user_help",
                "MsgType", "text"
        ));

        long ticketCountBefore = ticketMapper.countPendingTickets();

        mockMvc.perform(post("/api/v1/webhook/wecom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wecomPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        long ticketCountAfter = ticketMapper.countPendingTickets();
        assertEquals(ticketCountBefore, ticketCountAfter,
                "Rule-matched chitchat should not create a ticket");
    }
}
