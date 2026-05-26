# Architecture Constraints

## 分层架构

项目采用六层架构，严格单向依赖：上层可依赖下层，下层不可反向依赖上层。

```
┌──────────────────────────────────────────────┐
│ 表现层 (api/)       │ REST Controller        │
│                      │ 仅做参数校验和结果封装    │
├──────────────────────────────────────────────┤
│ 应用服务层 (service/application/)             │
│                      │ 编排业务流程             │
│                      │ 调用 domain + infra 层   │
├──────────────────────────────────────────────┤
│ 领域服务层 (service/domain/)                   │
│                      │ 核心业务逻辑             │
│                      │ 不依赖任何外部 API         │
├──────────────────────────────────────────────┤
│ 基础设施层 (infra/)   │ 外部集成适配              │
│                      │ Channel Adapter /        │
│                      │ Notification / AI / MQ   │
├──────────────────────────────────────────────┤
│ 数据访问层 (dao/)     │ MyBatis-Plus Mapper     │
│                      │ 纯数据访问，无业务逻辑       │
├──────────────────────────────────────────────┤
│ 通用工具层 (common/)  │ 工具类、常量、异常枚举      │
│                      │ 不依赖业务层              │
└──────────────────────────────────────────────┘
```

## 模块划分

```
im-ticket/
├── im-ticket-api/          # REST Controller
├── im-ticket-service/      # 业务服务
│   ├── application/        # 应用服务（编排）
│   │   ├── ticket/         # 工单应用服务
│   │   ├── routing/        # 智能路由应用服务
│   │   ├── notification/   # 通知应用服务
│   │   ├── duty/           # 值班应用服务
│   │   └── knowledge/      # 知识库应用服务
│   └── domain/             # 领域服务
│       ├── ticket/         # 工单状态机、工单分配
│       ├── routing/        # 规则引擎、意图识别
│       ├── notification/   # 模板引擎
│       ├── duty/           # 排班逻辑
│       └── knowledge/      # 知识检索
├── im-ticket-infra/        # 基础设施
│   ├── channel/            # Channel Adapter 抽象 + 实现
│   │   ├── ChannelAdapter.java          # 接口
│   │   ├── WeComChannelAdapter.java     # 企业微信
│   │   └── FeishuChannelAdapter.java    # 飞书
│   ├── ai/                 # Spring AI 集成
│   ├── mq/                 # RabbitMQ 配置
│   ├── cache/              # Redis 配置
│   └── notification/       # 通知发送实现（IM Bot、WebSocket）
├── im-ticket-dao/          # MyBatis-Plus Mapper + Entity
├── im-ticket-common/       # 工具类、常量、枚举、异常
└── im-ticket-web/          # Vue 3 前端
```

## 包内命名规范

| 层 | 类命名 | 示例 |
|-----|--------|------|
| Controller | `{Entity}Controller` | `TicketController`, `CapitalController` |
| Application Service | `{Entity}AppService` | `TicketAppService`, `RoutingAppService` |
| Domain Service | `{Domain}Service` | `TicketStateMachine`, `RuleEngineService` |
| Mapper | `{Entity}Mapper` | `TicketMapper` |
| Infra Adapter | `{Channel}ChannelAdapter` | `WeComChannelAdapter` |
| DTO | `{Entity}{Action}Req/Resp` | `TicketCreateReq`, `TicketListResp` |
| Enum | 大写蛇形 | `TICKET_CREATED`, `AGENT_ASSIGNED` |

## 接口约束

### RESTful API

- URL 前缀：`/api/v1/`
- 统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- 分页请求参数：`page`（从 1 开始）、`size`（默认 20，最大 100）
- 分页响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 128,
    "page": 1,
    "size": 20
  }
}
```

- GET 请求参数用 query string，POST/PUT 用 JSON body
- 时间格式统一使用 ISO 8601：`2026-05-26T10:30:00+08:00`
- 日期格式：`2026-05-26`

### Controller 约束

- Controller 只做三件事：接收参数 → 参数校验 → 调用 AppService → 返回结果
- 禁止在 Controller 中写业务逻辑
- 参数校验使用 `jakarta.validation` 注解，禁止在方法体内逐字段校验
- 登录用户信息从 `SecurityContextHolder` 获取，不接受前端传入的 userId

### Service 约束

- Application Service：编排多个 Domain Service 和 Infra Adapter 的调用，不包含领域逻辑
- Domain Service：纯业务逻辑，禁止依赖 HttpServletRequest、HttpServletResponse 等 Web 层对象
- Domain Service 的单元测试不启动 Spring 容器，直接 new 对象测试

## Channel Adapter 抽象

所有 IM 渠道实现 `ChannelAdapter` 接口：

```java
public interface ChannelAdapter {
    ChannelType getType();
    ChannelMessage normalize(Map<String, Object> rawPayload);
    void sendMessage(String channelUserId, String channelGroupId, MessageContent content);
    CapitalIdentity resolveCapital(Map<String, Object> rawPayload);
    boolean verifySignature(Map<String, Object> rawPayload, String signature);
}
```

新增渠道只需实现此接口并注册为 Spring Bean，无需修改业务逻辑。

## 状态机约束

工单状态转移必须通过 `TicketStateMachine` 领域服务，禁止在业务代码中手动修改 `ticket.status` 字段：

```java
// ✅ 正确：通过状态机
ticketStateMachine.transition(ticket, TicketEvent.AGENT_ACCEPT, agentId);

// ❌ 错误：直接修改
ticket.setStatus(TicketStatus.IN_PROGRESS);
ticketMapper.updateById(ticket);
```

所有状态转移事件通过 `TicketStateLog` 表记录流水（from_status, to_status, operator, timestamp）。

## 外部依赖治理

- 所有外部 API 调用（IM 渠道、LLM）必须设置超时：连接超时 3s，读取超时 10s
- 外部调用必须有重试机制（最多 3 次，指数退避 1s/2s/4s）
- 外部调用必须有降级逻辑，不依赖外部服务返回的异常状态正常工作
- 所有渠道回调必须在 Channel Adapter 层完成验签后再传递给业务层