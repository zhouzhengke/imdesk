# Testing Strategy

## 测试金字塔

```
        ┌──────┐
        │ E2E  │  ← 核心用户旅程，少量
        ├──────┤
        │ 集成  │  ← API + 数据库 + MQ + 外部 Mock
        ├──────┤
        │ 单元  │  ← 业务逻辑全覆盖
        └──────┘
```

## 单元测试

### 强制要求

- **Domain Service 必须 100% 单元测试覆盖**：状态机、规则引擎、意图分类逻辑、排班分配策略、知识检索逻辑
- 使用 JUnit 5 + Mockito，不启动 Spring 容器
- 外部依赖（Mapper、ChannelAdapter、LLM 调用）全部 Mock

### 必须覆盖的场景

| 测试类型 | 必测内容 |
|---------|---------|
| 正常路径 | 每个公开方法的正常输入 → 期望输出 |
| 边界条件 | null、空集合、空字符串、最大值/最小值 |
| 异常路径 | 外部依赖返回异常时的行为 |
| 状态转移 | 每个状态的每个合法转移 → 验证结果状态正确 |
| 非法转移 | 每个状态的非法转移 → 验证抛出 TicketException |

### 示例

```java
class TicketStateMachineTest {

    @Test
    void shouldTransitionFromPendingToInProgress() {
        Ticket ticket = createTicket(TicketStatus.PENDING);
        ticketStateMachine.transition(ticket, TicketEvent.AGENT_ACCEPT, "agent-zhangsan");
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals("agent-zhangsan", ticket.getAssignedAgentId());
    }

    @Test
    void shouldThrowWhenTransitionFromClosed() {
        Ticket ticket = createTicket(TicketStatus.CLOSED);
        assertThrows(TicketException.class, () ->
            ticketStateMachine.transition(ticket, TicketEvent.AGENT_ACCEPT, "agent-zhangsan"));
    }

    @Test
    void shouldThrowWhenDuplicateAssign() {
        Ticket ticket = createTicket(TicketStatus.IN_PROGRESS, "agent-lisi");
        assertThrows(TicketException.class, () ->
            ticketStateMachine.transition(ticket, TicketEvent.AGENT_ACCEPT, "agent-zhangsan"));
    }

    @Test
    void shouldTransitionAllValidPaths() {
        // 待处理 → 处理中
        // 处理中 → 已解决 / 已转交 / 已驳回 / 已延期
        // 已转交 → 处理中 / 待处理（超时）
        // ... 覆盖所有 22 条合法转移
    }
}
```

### 规则引擎测试

```java
class RuleEngineServiceTest {

    @Test
    void shouldMatchPrefixCommand() { ... }
    @Test
    void shouldMatchKeyword() { ... }
    @Test
    void shouldMatchTicketNoPattern() { ... }
    @Test
    void shouldReturnEmptyWhenNoMatch() { ... }
    @Test
    void shouldNotMatchSimilarButDifferentPattern() { ... }
}
```

## 集成测试

### 范围

集成测试启动 Spring 容器，使用 Testcontainers 或 H2/Embedded 替代真实依赖：

| 外部依赖 | 测试替代 |
|---------|---------|
| MySQL | H2 MySQL Mode 或 Testcontainers MySQL |
| Redis | Embedded Redis 或 Testcontainers Redis |
| RabbitMQ | Testcontainers RabbitMQ 或 Mock AMQP Template |
| Elasticsearch | Testcontainers Elasticsearch |
| LLM API | WireMock 模拟 LLM 响应 |
| IM 渠道 API | WireMock 模拟企微/飞书回调 |

### 必须覆盖的集成场景

- Controller → Service → Mapper 完整链路（CRUD）
- 消息队列：发送 → 消费 → 确认，死信队列重试
- 渠道 Webhook 入口：验签 → 消息转换 → 路由 → 响应
- 多线程场景：重复分配工单（用分布式锁防并发）
- 定时任务：超时检测、到期提醒

### 示例

```java
@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Test
    void shouldCreateTicketAndAssignAgent() { ... }
    @Test
    void shouldReturnErrorWhenNoAgentAvailable() { ... }
    @Test
    void shouldNotAssignDuplicateTickets() { ... }
}

@SpringBootTest
class RoutingIntegrationTest {

    @Test
    void shouldRouteToQueryByPrefix() { ... }
    @Test
    void shouldRouteToLLMWhenRuleNotMatch() { ... }
    @Test
    void shouldFallbackToCreateTicketWhenLLMTimeout() { ... }
}
```

## E2E 测试（Phase 1）

覆盖 3 条核心用户旅程：

### 旅程 1：知识库自动应答

```
[模拟企微 Webhook] → 发送"审批流程是什么？"
  → 规则引擎未命中 → LLM 识别 knowledge_query
  → FAQ 命中 → 返回答案
  → 验证：无工单创建，回复内容包含"审批"
```

### 旅程 2：知识库升级为工单

```
[模拟企微 Webhook] → 连续发送 3 条知识类问题
  → 第 3 条触发升级阈值
  → 系统自动创建工单 → 分配给值班客服
  → 验证：工单已创建，客服收到通知，KB 对话已带入工单上下文
```

### 旅程 3：工单完整生命周期

```
[模拟企微 Webhook] → 发送问题（非 KB 类）
  → 创建工单 → 分配给值班 → 客服领取 → 客服回复
  → 客服标记已解决 → 通知用户确认
  → 用户确认 → 工单关闭 → 摘要入库
  → 验证：状态转移链完整，通知正确发送，历史工单库有记录
```

## 测试覆盖率要求

| 层级 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|---------|-----------|------|
| domain/ | ≥ 90% | ≥ 85% | 核心业务逻辑 |
| application/ | ≥ 70% | ≥ 65% | 编排层，关键路径必测 |
| infra/ | ≥ 50% | — | 外部集成，集成测试覆盖为主 |
| api/ | ≥ 60% | — | Controller 层，集成测试覆盖为主 |

使用 JaCoCo 生成覆盖率报告，CI 构建中覆盖率不达标则构建失败。

## 测试命名规范

```
方法名_场景_期望结果

示例：
transition_PendingToInProgress_shouldSucceed
transition_ClosedToAnyState_shouldThrow
routeMessage_wecomTextWithHashPrefix_shouldReturnQueryIntent
routeMessage_wecomNaturalLanguage_shouldCallLLM
routeMessage_llmTimeout_shouldFallbackToCreateTicket
```

## 禁止事项

- ❌ 单元测试依赖 Spring 容器（加 `@SpringBootTest` 就不是单元测试）
- ❌ 测试依赖执行顺序（每个测试必须独立，不依赖其他测试的状态）
- ❌ 测试用例之间共享可变状态
- ❌ 使用 `Thread.sleep()` 等待异步结果（用 Awaitility 库）
- ❌ 在 CI 环境跳过测试
- ❌ 提交代码前不跑本地测试