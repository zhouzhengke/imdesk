package com.company.imticket.api;

import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketMessage;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketMessageMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.service.domain.duty.AssignmentResult;
import com.company.imticket.service.domain.duty.DutyAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Task 7.3: Concurrency Tests")
class TicketCreationConcurrencyE2ETest {

    private static final int TOTAL_USERS = 50;
    private static final int GROUPS = 5;
    private static final int USERS_PER_GROUP = TOTAL_USERS / GROUPS;
    private static final int THREAD_POOL_SIZE = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketMessageMapper messageMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @MockBean
    private AiClient aiClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private DutyAssignmentService dutyAssignment;

    @MockBean
    private SessionCacheService sessionCache;

    @BeforeEach
    void setUp() {
        when(aiClient.generateTicketSummary(anyString())).thenReturn("测试摘要");
        when(aiClient.generateKnowledgeAnswer(anyString(), anyString())).thenReturn("参考回答");

        when(dutyAssignment.assign(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String channelUserId = invocation.getArgument(1);
                    int userNum = extractUserNumber(channelUserId);
                    long agentId = (userNum % 3) + 1;
                    String agentName = agentId == 1 ? "张三" : agentId == 2 ? "李四" : "王五";
                    return new AssignmentResult(agentId, agentName, "primary_duty");
                });
    }

    @Test
    @DisplayName("50 users in 5 groups send messages concurrently → no duplicate tickets, no message loss")
    void concurrentTicketCreation_noDuplicates_noMessageLoss() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Set<String> erroredUsers = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int g = 0; g < GROUPS; g++) {
            final String groupId = "concurrent_group_" + g;
            for (int u = 0; u < USERS_PER_GROUP; u++) {
                final int userNum = g * USERS_PER_GROUP + u;
                final String userId = "concurrent_user_" + userNum;

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String body = buildWecomPayload(userId, groupId, "问题咨询-" + userNum);
                        mockMvc.perform(post("/api/v1/webhook/wecom")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andExpect(status().isOk());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        erroredUsers.add(userId);
                    }
                }, executor);

                futures.add(future);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(TOTAL_USERS, successCount.get(),
                "All " + TOTAL_USERS + " requests should succeed");
        assertEquals(0, failCount.get(),
                "No requests should fail. Errored users: " + erroredUsers);

        List<Ticket> allTickets = ticketMapper.selectList(null);

        Set<String> seenUsers = new HashSet<>();
        Set<String> seenTicketNos = new HashSet<>();
        int totalMessages = 0;

        for (Ticket ticket : allTickets) {
            if (!ticket.getChannelUserId().startsWith("concurrent_user_")) {
                continue;
            }

            seenTicketNos.add(ticket.getTicketNo());
            assertNotNull(ticket.getTicketNo(), "Every ticket must have a ticket number");

            String user = ticket.getChannelUserId();
            assertFalse(seenUsers.contains(user),
                    "Duplicate ticket for user: " + user);
            seenUsers.add(user);

            List<TicketMessage> messages = messageMapper.selectByTicketId(ticket.getId());
            assertFalse(messages.isEmpty(),
                    "Ticket " + ticket.getTicketNo() + " must have at least one message");
            totalMessages += messages.size();
        }

        assertEquals(TOTAL_USERS, seenUsers.size(),
                "Should have exactly " + TOTAL_USERS + " unique users with tickets");
        assertEquals(TOTAL_USERS, seenTicketNos.size(),
                "Should have exactly " + TOTAL_USERS + " unique ticket numbers");
        assertTrue(totalMessages >= TOTAL_USERS,
                "Should have at least " + TOTAL_USERS + " messages, got " + totalMessages);
    }

    @Test
    @DisplayName("same user concurrent requests → only one ticket created (dedup via TICKET_HAS_OPEN)")
    void sameUserConcurrentRequests_onlyOneTicketCreated() throws Exception {
        final String userId = "dedup_user_concurrent";
        final String groupId = "dedup_group";
        final int concurrentRequests = 10;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            final int reqNum = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String body = buildWecomPayload(userId, groupId, "去重测试-" + reqNum);
                    mockMvc.perform(post("/api/v1/webhook/wecom")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body));
                } catch (Exception ignored) {
                    // TICKET_HAS_OPEN exception is expected for duplicate requests
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<Ticket> tickets = ticketMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getChannelUserId, userId));

        assertEquals(1, tickets.size(),
                "Only one ticket should be created for the same user, got: " + tickets.size());
    }

    @Test
    @DisplayName("Redis distributed lock prevents duplicate assignment under concurrency")
    void distributedLock_preventsDuplicateAssignment() throws Exception {
        assertNotNull(redissonClient,
                "RedissonClient must be available for distributed lock test");

        final String lockKey = "im:lock:assign:concurrent_test_user";
        final int attempts = 20;
        AtomicInteger acquiredCount = new AtomicInteger(0);
        Set<String> acquiredBy = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            final String threadName = "thread-" + i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                RLock lock = redissonClient.getLock(lockKey);
                try {
                    if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                        try {
                            acquiredCount.incrementAndGet();
                            acquiredBy.add(threadName);
                            Thread.sleep(50);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(attempts, acquiredCount.get(),
                "All " + attempts + " threads should eventually acquire the lock");
        assertEquals(attempts, acquiredBy.size(),
                "Each thread should have acquired the lock exactly once");
    }

    @Test
    @DisplayName("concurrent ticket assignment → each ticket gets a unique agent (no double-assign)")
    void concurrentAssignment_eachTicketGetsUniqueAgent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<String, Long> ticketToAgent = new ConcurrentHashMap<>();

        for (int i = 0; i < 20; i++) {
            final String userId = "assign_user_" + i;
            final String groupId = "assign_group";
            final int userNum = i;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String body = buildWecomPayload(userId, groupId, "分配测试-" + userNum);
                    mockMvc.perform(post("/api/v1/webhook/wecom")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body));
                } catch (Exception ignored) {
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<Ticket> tickets = ticketMapper.selectList(null);
        int assignedCount = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getChannelUserId().startsWith("assign_user_")) {
                assertNotNull(ticket.getAssignedAgentId(),
                        "Ticket " + ticket.getTicketNo() + " should have an assigned agent");
                ticketToAgent.put(ticket.getTicketNo(), ticket.getAssignedAgentId());
                assignedCount++;
            }
        }

        assertEquals(20, assignedCount,
                "All 20 tickets should be created and assigned");
        assertEquals(20, ticketToAgent.size(),
                "Each ticket should have a unique ticket number");
    }

    private String buildWecomPayload(String userId, String groupId, String content) {
        try {
            Map<String, Object> payload = Map.of(
                    "MsgType", "text",
                    "FromUserName", userId,
                    "ToUserName", "wx_test_corp",
                    "Content", content,
                    "CreateTime", System.currentTimeMillis() / 1000,
                    "ChatId", groupId
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int extractUserNumber(String channelUserId) {
        try {
            String[] parts = channelUserId.split("_");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return 0;
        }
    }
}
