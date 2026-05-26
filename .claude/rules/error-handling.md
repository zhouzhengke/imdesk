# Error Handling & Logging Standards

## 异常体系

### 异常枚举

所有业务异常必须使用 `BizErrorCode` 枚举定义，禁止硬编码错误码和消息字符串：

```java
public enum BizErrorCode {
    // 通用
    SUCCESS(0, "success"),
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或 Token 已过期"),
    FORBIDDEN(40300, "无操作权限"),
    NOT_FOUND(40400, "资源不存在"),
    INTERNAL_ERROR(50000, "系统内部错误"),

    // 工单 (10xxx)
    TICKET_NOT_FOUND(10001, "工单不存在"),
    TICKET_STATUS_ILLEGAL(10002, "当前工单状态不允许此操作"),
    TICKET_ALREADY_ASSIGNED(10003, "工单已被其他客服领取"),
    TICKET_HAS_OPEN(10004, "该会话已存在未关闭工单"),

    // 路由 (11xxx)
    ROUTING_LLM_TIMEOUT(11001, "大模型路由超时"),
    ROUTING_LLM_ERROR(11002, "大模型路由服务异常"),
    ROUTING_RULE_PARSE_ERROR(11003, "路由规则解析失败"),

    // 知识库 (12xxx)
    KB_FAQ_DUPLICATE(12001, "FAQ 问题已存在"),
    KB_DOC_PARSE_ERROR(12002, "文档解析失败"),
    KB_VECTOR_STORE_ERROR(12003, "向量存储异常"),
    KB_SEARCH_TIMEOUT(12004, "知识库搜索超时"),

    // 渠道 (13xxx)
    CHANNEL_SIGNATURE_INVALID(13001, "渠道回调签名验证失败"),
    CHANNEL_SEND_FAILED(13002, "渠道消息发送失败"),
    CHANNEL_UNSUPPORTED(13003, "不支持的渠道类型"),
    CHANNEL_USER_NOT_FOUND(13004, "渠道用户未找到"),

    // 值班 (14xxx)
    DUTY_NO_AGENT_AVAILABLE(14001, "当前无可用的值班客服"),
    DUTY_SHIFT_CONFLICT(14002, "排班时间冲突"),

    // 通知 (15xxx)
    NOTIFICATION_TEMPLATE_NOT_FOUND(15001, "通知模板不存在"),
    NOTIFICATION_SEND_FAILED(15002, "通知发送失败"),
    NOTIFICATION_TEMPLATE_PARSE_ERROR(15003, "通知模板解析失败");
}
```

### 异常类层次

```
RuntimeException
└── BizException          ← 业务异常（errorCode + 可选的 detail）
    ├── TicketException   ← 工单相关异常
    ├── RoutingException  ← 路由相关异常
    ├── ChannelException  ← 渠道相关异常
    └── DutyException     ← 值班相关异常
```

### 异常抛出规范

```java
// ✅ 正确：抛出业务异常
throw new TicketException(BizErrorCode.TICKET_STATUS_ILLEGAL,
    String.format("ticket=%s, current=%s, target=%s", ticketNo, from, to));

// ✅ 正确：使用带 detail 的重载
throw new ChannelException(BizErrorCode.CHANNEL_SEND_FAILED,
    "企微消息发送失败，msgId=" + msgId);

// ❌ 错误：直接 new RuntimeException("...")
throw new RuntimeException("工单状态不对");

// ❌ 错误：返回 null 或 boolean 代替异常
return null;
```

## 全局异常处理

使用 `@RestControllerAdvice` 统一处理：

| 异常 | HTTP 状态码 | code | 额外处理 |
|------|-----------|------|---------|
| `BizException` | 200 | error.code | 根据 code 决定是否 WARN |
| `MethodArgumentNotValidException` | 200 | 40001 | 拼接字段校验错误详情 |
| `HttpMessageNotReadableException` | 200 | 40001 | JSON 格式错误 |
| `AccessDeniedException` | 200 | 40300 | 记录用户和资源 ID |
| `Exception`（兜底） | 200 | 50000 | ERROR 级别日志 + 隐藏内部错误详情 |

生产环境不向客户端暴露 `e.getMessage()` 的原始堆栈信息。兜底异常只返回 "系统繁忙，请稍后重试"。

## 日志规范

### 日志框架

使用 SLF4j + Logback，禁止在代码中直接使用 `System.out.println` 或 `e.printStackTrace()`。

### 日志级别使用

| 级别 | 场景 | 示例 |
|------|------|------|
| ERROR | 需要人工介入的异常 | 数据库连接失败、MQ 宕机、LLM 连续 3 次重试失败、渠道验签失败 |
| WARN | 可自动恢复的异常 | 单次 LLM 调用超时（将重试）、一次消息发送失败（将重试）、值班无人在线告警 |
| INFO | 关键业务节点 | 工单创建/状态变更、消息路由结果、通知发送、客服操作（转交/关闭） |
| DEBUG | 调试细节 | 规则匹配过程、KB 检索召回文档数、状态机转移详情 |

### 日志格式

生产环境使用 JSON 格式，包含 `timestamp`, `level`, `logger`, `thread`, `message`, `mdc`：

```json
{"timestamp":"2026-05-26T10:30:00.123+08:00","level":"INFO","logger":"c.i.t.s.domain.ticket.TicketStateMachine",
 "thread":"http-nio-8082-exec-3","message":"ticket state transition: IM-20260526-0001, PENDING → IN_PROGRESS",
 "mdc":{"ticketNo":"IM-20260526-0001","userId":"agent-zhangsan","traceId":"a1b2c3d4"}}
```

### MDC 要求

以下字段必须在请求入口（Filter/Interceptor）写入 MDC，确保每个业务日志条目可追踪：

- `traceId`：请求链路 ID（从请求头或自行生成 UUID）
- `userId`：当前操作人 ID（客服或资方用户标识）
- `ticketNo`：工单编号（工单相关操作必须携带）

### 日志内容规范

- INFO 日志必须包含操作的业务对象和结果：`"工单创建成功, ticketNo=IM-20260526-0001, channel=wecom, capital=XX银行"`
- WARN/ERROR 必须包含足够的上下文来定位问题：关键参数 + 环境信息
- 禁止在循环中打印 INFO 级别日志（用 DEBUG 或批量汇总）
- 禁止打印敏感信息（手机号、身份证、密码）到日志，必须脱敏
- 禁止打印完整的请求/响应 body 到 INFO（大文本消息截断到 200 字符）

## 降级与容错

### LLM 调用降级

```
LLM 调用失败 / 超时
  → 重试（最多 3 次，指数退避 1s/2s/4s）
  → 仍失败 → 降级为纯规则引擎路由
  → 规则也无法匹配 → 默认创建工单（兜底安全策略）
```

### 渠道消息发送降级

```
消息发送失败
  → 重试（最多 3 次，间隔 2s）
  → 仍失败 → 记录失败消息到 `message_fail_queue` 表
  → 定时任务（每 5 分钟）重试失败队列
  → 超过 1 小时仍未成功 → 告警通知管理员
```

### 值班分配降级

```
主值班 → 超时未响应 → 备选值班 → 超时 → 待分配池 + 告警
```

降级链中的每一步都要记录 WARN 日志并触发通知。

## 监控埋点

Phase 1 至少以下指标需要输出到日志（后续可对接 Prometheus）：

- 工单创建量（按渠道/资方/小时）
- 工单平均处理时长（创建 → 关闭）
- LLM 调用次数、成功率、平均耗时
- 规则引擎命中率
- 知识库检索命中率（L1/L2/L3 各自命中率）
- 消息发送成功率（按渠道）
- 值班超时率
- 待分配池积压数量