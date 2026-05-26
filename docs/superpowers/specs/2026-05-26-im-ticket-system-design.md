# IM工单系统设计文档

> 版本: v1.0 | 日期: 2026-05-26 | 状态: 设计完成

## 1. 项目概述

### 1.1 背景

客服与技术支持团队需要在多渠道 IM（企业微信、飞书等）上处理来自合作资方的用户咨询。现有方式依赖人工值守，存在响应不及时、问题跟踪困难、跨渠道割裂等问题。

### 1.2 产品目标

- **统一收口**：多渠道 IM 对话统一进入工单系统
- **智能路由**：规则引擎 + 大模型识别用户意图，自动分流
- **自动化工单**：用户问题自动创建工单，关联渠道、资方、会话上下文
- **智能值班**：根据预设规则自动分配待处理消息
- **知识库问答**：FAQ + 文档 RAG + 历史工单，三层检索自动回复
- **便捷查询**：用户通过自然语言或指令查询工单状态

### 1.3 Phase 1 规模

- 10-20 个资方
- 20-50 个客服
- 日工单量 100-500
- 首发渠道：企业微信、飞书
- 部署模式：多实例高可用

## 2. 用户角色

| 角色 | 描述 | 主要操作 |
|------|------|---------|
| 资方用户 | 合作金融机构/资金方的工作人员 | 发起咨询、查询工单进度、接收知识库回复 |
| 客服/技术支持 | 处理工单的内部人员 | 接收、回复、转交、驳回、延期、关闭工单 |
| 系统管理员 | 管理后台配置 | 配置渠道、值班规则、通知模板、知识库、资方映射 |
| 值班负责人 | 排班管理与消息调度 | 设置值班表、监控未分配消息、手动指派 |

## 3. 系统架构

### 3.1 六层架构

```
┌─────────────────────────────────────────┐
│ 展现层    │ Vue 3 + Element Plus 工作台  │
│           │ /tickets · /duty · /admin    │
├─────────────────────────────────────────┤
│ 通知层    │ 内部通知（工作台推送 + IM Bot）│
│           │ 外部通知（回源IM渠道 + 模板）  │
├─────────────────────────────────────────┤
│ 业务层    │ 工单生命周期 · 资方管理       │
│           │ 知识库问答 · 查询服务         │
├─────────────────────────────────────────┤
│ 智能路由层│ 规则引擎（毫秒级）→ 大模型兜底 │
│           │ 意图识别 · 实体抽取 · 升级判断 │
├─────────────────────────────────────────┤
│ 接入层    │ 企微/飞书 Webhook 接入       │
│           │ 消息格式统一 · 验签 · 资方识别 │
├─────────────────────────────────────────┤
│ 数据层    │ MySQL · Redis · RabbitMQ     │
│           │ Elasticsearch（向量检索）     │
└─────────────────────────────────────────┘
```

### 3.2 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Java 17+ / Spring Boot 3.x | 企业级基础框架 |
| AI 集成 | Spring AI | 统一大模型接入抽象层，支持切换模型供应商 |
| 状态机 | Spring StateMachine / Enum 驱动 | 工单生命周期管理 |
| 数据库 | MySQL 8.x | 业务数据主存储 |
| 缓存 | Redis | 会话缓存、在线状态、分布式锁 |
| 消息队列 | RabbitMQ | IM 消息缓冲、通知异步发送、削峰 |
| 搜索引擎 | Elasticsearch | 知识库向量检索（文档 + 历史工单） |
| 前端 | Vue 3 + Element Plus + Pinia | 客服工作台 |
| 实时通信 | WebSocket (STOMP) | 工作台消息实时推送 |
| 部署 | Nginx + 多实例 | 负载均衡、高可用 |

### 3.3 预留扩展点（Phase 1 不实现）

- OAuth 2.0 用户体系对接接口
- 业务系统 API 网关（对外提供标准 RESTful 接口）
- 钉钉 / Slack 渠道适配器
- 流程引擎接口（转交等复杂场景未来可局部引入）

## 4. 智能路由引擎

### 4.1 核心策略：规则优先 + 大模型兜底

```
IM 消息 → 规则引擎（毫秒级）→ 命中？→ 直接路由
                ↓ 未命中
         大模型意图识别 → 返回结构化意图 → 路由
                ↓ 模型不可用
         降级规则 → 默认创建工单
```

### 4.2 规则引擎

- 前缀匹配：`#查单`、`#我的工单`、`#帮助`
- 关键词匹配：`转人工`、`联系客服`、`创建工单`
- 正则匹配：工单编号格式 `IM-\d{8}-\d{4}`
- 规则配置通过后台管理界面维护，支持热更新

### 4.3 大模型意图识别

调用 LLM，输入上下文信息，输出结构化 JSON：

```json
{
  "intent": "ticket_query | create_ticket | supplement | knowledge_query | business_query | chitchat | urgent_escalation",
  "confidence": 0.85,
  "entities": {
    "ticket_id": "IM-20250526-0001",
    "keywords": ["审批", "流程"]
  },
  "priority": "normal | high | urgent",
  "sentiment": "positive | neutral | negative"
}
```

Prompt 上下文包含：资方名称、用户名、当前是否有未关闭工单、近期 KB 交互轮次。

### 4.4 升级策略（KB 问答 → 工单）

| 触发条件 | 判断方式 | 可配置 | 优先级 |
|---------|---------|--------|--------|
| 用户主动要求转人工 | 规则引擎 + LLM 近义识别 | 关键词列表 | 最高 |
| LLM 判断 KB 无法回答 | confidence 低于阈值 / sentiment 负面 | 阈值可配 | 高 |
| KB 交互轮次超限（≥ N 轮） | 会话计数器 | N=3（默认） | 中 |
| LLM 直接识别为 create_ticket | intent 分类 | N/A | 中 |

升级时，KB 对话历史自动带入工单上下文，客服无需重新询问。

## 5. 知识库与智能问答

### 5.1 三层检索架构

| 层级 | 数据源 | 检索方式 | 响应速度 | 说明 |
|------|--------|---------|---------|------|
| L1 | FAQ 库（MySQL） | 精确/模糊匹配 | 毫秒级 | 管理员配置的 Q&A 对 |
| L2 | 文档库（ES 向量索引） | 向量相似度检索 + LLM 生成 | ~500ms | PDF/Word/Markdown 上传后自动切片 |
| L3 | 历史工单库（ES 向量索引） | 向量相似度检索 + LLM 生成 | ~500ms | 已关闭工单的摘要 + 解决方案 |

检索流程：L1 → 未命中 → L2 → 未命中 → L3 → 均未命中 → 触发升级策略创建工单

### 5.2 历史工单入库

```
工单关闭 → LLM 自动摘要提取（问题、方案、关键词）
         → 人工审核（可选，管理员确认 + 脱敏检查）
         → 向量化存储至 ES
```

### 5.3 辅助能力

- **客服工作台相似工单推荐**：工单详情面板展示 Top3 相似历史工单
- **一键转 FAQ**：高频相似工单可由管理员一键转为 FAQ
- **Prompt 增强**：回答时向 LLM 注入相似历史工单的解决方案

## 6. 工单生命周期

### 6.1 状态定义（8 状态）

| 状态 | 英文标识 | 说明 |
|------|---------|------|
| 待处理 | PENDING | 工单已创建，等待分配/领取 |
| 处理中 | IN_PROGRESS | 客服已接单，正在处理 |
| 已转交 | TRANSFERRED | 转交另一客服，等待对方接单 |
| 已驳回 | REJECTED | 客服驳回，返回待分配池 |
| 已延期 | DEFERRED | 需等待外部反馈，到期自动提醒 |
| 已解决 | RESOLVED | 客服标记问题已解决 |
| 待确认 | WAITING_CONFIRM | 等待用户确认，默认 X 天自动关闭 |
| 已关闭 | CLOSED | 终态，不可再修改 |

### 6.2 状态转移规则

```
PENDING        → IN_PROGRESS     (客服领取 / 系统分配)

IN_PROGRESS    → RESOLVED        (客服标记解决)
IN_PROGRESS    → TRANSFERRED     (转交他人)
IN_PROGRESS    → REJECTED        (驳回，不属于自己)
IN_PROGRESS    → DEFERRED        (延期等待外部反馈)

TRANSFERRED    → IN_PROGRESS     (新客服领取)
TRANSFERRED    → PENDING         (超时未领取退回)

REJECTED       → PENDING         (自动返回分配池)

DEFERRED       → IN_PROGRESS     (恢复处理 / 到期提醒)

RESOLVED       → WAITING_CONFIRM (通知用户确认)
RESOLVED       → CLOSED          (跳过确认直接关闭)

WAITING_CONFIRM → CLOSED         (用户确认 / 超时自动)
WAITING_CONFIRM → IN_PROGRESS    (用户反馈未解决，重开)

CLOSED          → (终态)
```

### 6.3 引擎选型

采用 **纯状态机**（Spring StateMachine 或 Enum 驱动的状态转移表），不引入流程引擎。

理由：工单状态转移都是确定的、单步的，没有并行分支、会签审批、条件网关等复杂场景。状态机轻量、可内聚到 Service 层、方便测试和调试。

每个状态转移携带 Event 钩子：通知发送、操作日志记录、工单流水写入。

## 7. 通知系统

### 7.1 双通道设计

| 维度 | 内部通知（→ 客服） | 外部通知（→ 资方用户） |
|------|-------------------|----------------------|
| 通知渠道 | 工作台推送 + IM Bot 私聊 + 群通知 | 回源到原始 IM 渠道 |
| 典型场景 | 新工单分配、超时告警、待分配提醒 | 工单创建确认、状态变更、解决通知 |
| 格式 | Markdown（飞书卡片）/ Text（企微） | 根据渠道适配 |
| 可回复 | 否（单向通知） | 是（用户可继续对话） |

### 7.2 回源通知原则

用户在哪个渠道提问，通知就回到哪个渠道：
- 企微群聊提问 → 通知回复在该企微群
- 飞书私聊提问 → 通知回复在该飞书会话

### 7.3 模板引擎

- 后台可配置通知模板，支持变量占位符
- 变量池：`{{ticket_id}}` · `{{ticket_status}}` · `{{capital_name}}` · `{{user_name}}` · `{{agent_name}}` · `{{category}}` · `{{summary}}` · `{{priority}}` · `{{created_at}}` · `{{resolved_at}}` · `{{channel_name}}` · `{{reply_content}}`
- 渠道格式适配：企微 Text、飞书 Markdown 卡片、工作台 WebSocket 推送

### 7.4 触发事件

| 事件 | 内部通知 | 外部通知 |
|------|---------|---------|
| 工单创建 | 通知值班客服（工作台 + IM） | 通知用户（回源渠道） |
| 工单分配 | 通知被分配的客服 | — |
| 客服回复 | — | 消息双向同步（非通知） |
| 工单解决 | 更新状态 | 通知用户（回源渠道） |
| 工单关闭 | — | 通知用户 |
| 值班超时 | 通知备选值班 + 管理员群告警 | — |
| 待分配积压 | 通知值班负责人 | — |

## 8. 多渠道对接

### 8.1 渠道适配层（Channel Adapter）

各渠道通过官方 API / Webhook 接入，统一转换为系统内部消息格式：

```json
{
  "message_id": "uuid",
  "channel": "wecom | feishu | dingtalk | slack",
  "channel_user_id": "external_user_id",
  "channel_group_id": "group_or_chat_id",
  "user_name": "display_name",
  "content": {"type": "text | image | file | mixed", "body": "..."},
  "timestamp": "2026-05-26T10:30:00Z",
  "raw_payload": "{...}"
}
```

### 8.2 资方识别映射

| 渠道 | 识别方式 | 配置示例 |
|------|---------|---------|
| 企业微信 | 外部联系人标签 / CorpId + UserId / 群聊 ID | 标签"XX银行" → 资方"XX银行" |
| 飞书 | 租户 tenant_key / 部门 / 自定义字段 | tenant_key=xxx → 资方"YY金服" |

支持一个资方对应多个渠道入口，工单数据在全局统一。

### 8.3 Phase 分步支持

| 阶段 | 渠道 | 支持能力 |
|------|------|---------|
| Phase 1 | 企业微信、飞书 | 接收消息、发送回复、身份识别、消息卡片 |
| Phase 2 | 钉钉、Slack | 机器人回调、Markdown/文本回复 |

## 9. 客服工作台

### 9.1 技术方案

- **框架**：Vue 3 + Element Plus + Pinia
- **实时通信**：WebSocket (STOMP over SockJS) 连接后端消息总线
- **布局**：左侧导航 + 中间列表 + 右侧详情面板（三栏布局）

### 9.2 页面结构

| 页面 | 路由 | 角色 | 功能 |
|------|------|------|------|
| 工单工作台 | /tickets | 客服 | 工单列表（筛选+搜索）、工单详情（对话记录、内部备注、相似工单推荐）、实时回复、转交/驳回/延期/解决/关闭 |
| 值班面板 | /duty | 值班负责人/管理员 | 当前值班状态、待分配会话数、超时告警、手动指派、在线/离线状态 |
| 系统管理 | /admin | 管理员 | 资方管理、渠道配置、值班排班、通知模板、知识库管理、规则引擎配置 |

### 9.3 工单工作台布局

```
┌──────────┬────────────────────────┬─────────────┐
│ 导航栏   │ 工单列表 + 搜索/筛选   │ 工单详情    │
│          │                        │             │
│ 工单列表 │ ┌────────────────────┐ │ 编号/资方   │
│ 实时会话 │ │ 表头: 编号|资方|... │ │ 状态/负责   │
│ 值班面板 │ ├────────────────────┤ │ 标签        │
│ 数据统计 │ │ IM-xxx | XX银行... │ │             │
│ ──────── │ │ IM-xxx | YY金服... │ │ 相似工单    │
│ 系统管理 │ │ ...                 │ │ Top 3       │
│ ·资方管理│ └────────────────────┘ │             │
│ ·渠道配置│ 分页器                 │ 对话记录    │
│ ·值班排班│                        │             │
│ ·通知模板│                        │ [回复][转交]│
│ ·知识库  │                        │  [解决]     │
└──────────┴────────────────────────┴─────────────┘
```

## 10. 值班引擎

### 10.1 排班管理

- 支持按日/周配置值班表（早班、晚班、周末班）
- 每个班次：1 个主值班 + N 个备选值班
- 值班人员从客服组成员中选择

### 10.2 消息分配策略（优先级递减）

1. **上下文继承**：当前会话上一次处理的客服（如有）
2. **主值班分配**：当前时段主值班人员
3. **备选值班**：主值班超时未响应（默认 5 分钟），转备选
4. **待分配池**：无人在线 → 进入待分配池 + 管理员群告警
5. **手动领取**：备选值班可主动领取超时会话

### 10.3 在线状态

Phase 1：系统内手动签到（上班/休息）。预留 IM 状态 API 对接接口。

## 11. 非功能需求

| 项目 | 要求 |
|------|------|
| 响应时效 | 用户消息到系统处理完成 ≤ 2s；规则引擎路由 ≤ 50ms |
| 消息可靠性 | RabbitMQ 持久化 + 消费确认，消息不丢失；消息数据保留 ≥ 180 天 |
| 并发能力 | 支持 1000+ 活跃会话（单渠道） |
| 安全性 | 渠道回调验签；敏感信息脱敏显示；SQL 注入/XSS 防护 |
| 可扩展性 | 渠道适配器抽象接口，新渠道只需实现 Adapter |
| 值班高可用 | 分配服务多实例部署，Redis 分布式锁避免重复分配 |

## 12. 数据模型概要

### 12.1 核心实体

- **Capital（资方）**：name, contact, contract_period, status
- **CapitalChannelMapping（资方渠道映射）**：capital_id, channel, channel_identifier, identifier_type
- **Ticket（工单）**：ticket_no, capital_id, channel, channel_user_id, channel_group_id, user_name, description, status, assigned_agent_id, priority, category, tags, context_summary, created_at, resolved_at, closed_at
- **TicketMessage（工单消息）**：ticket_id, sender_type(USER/AGENT/SYSTEM), content, content_type, created_at
- **TicketStateLog（状态流水）**：ticket_id, from_status, to_status, operator_id, operator_type, remark, created_at
- **Agent（客服）**：name, status(ONLINE/OFFLINE/BREAK), im_accounts
- **Shift（班次）**：name, start_time, end_time, primary_agent_ids, backup_agent_ids
- **ShiftSchedule（排班）**：date, shift_id, agents
- **NotificationTemplate（通知模板）**：code, name, direction(INTERNAL/EXTERNAL), channel, format, template_content
- **KnowledgeFaq（FAQ）**：question, answer, keywords, category, enabled
- **KnowledgeDocument（文档）**：title, file_type, file_path, chunk_count, vector_index_id
- **KnowledgeTicketArchive（历史工单归档）**：ticket_id, summary, solution, keywords, vector_index_id, reviewed_by

### 12.2 索引策略

- Ticket: (status, assigned_agent_id), (capital_id, created_at), (channel, created_at)
- TicketMessage: (ticket_id, created_at)
- KnowledgeFaq: (keywords, FULLTEXT)
- KnowledgeTicketArchive: vector_index_id (ES 向量索引)

## 附录 A：群聊交互完整流程

```
Round 1: 用户在群内问"额度审批流程是什么？"
  → 规则引擎未命中 → LLM 识别 knowledge_query
  → FAQ 命中 → 群内回复 → 无工单创建

Round 2: 用户追问"需要哪些材料？"
  → LLM 识别 knowledge_query → RAG 检索文档
  → 群内回复 → 系统记录：2 轮 KB 交互

Round 3: 用户"我的情况比较特殊，能帮我看下吗？"
  → LLM 识别 intent=create_ticket + sentiment=negative + ≥阈值
  → 触发升级 → 自动创建工单 IM-20250526-0001
  → 自动回复用户"您的问题已创建工单（XXX）"
  → 通知值班客服张三
  → KB 对话历史自动带入工单上下文

Round 4+: 客服张三在群内回复 → 消息同步 → 后续对话自动追加到工单
```

## 附录 B：术语对照

| 中文 | 英文 | 说明 |
|------|------|------|
| 资方 | Capital / Partner | 合作金融机构 |
| 工单 | Ticket | 用户问题追踪单元 |
| 值班 | On-call / Duty | 客服排班机制 |
| 知识库 | Knowledge Base | FAQ + 文档 + 历史工单 |
| 回源通知 | Source-channel Notification | 通知回到用户原始 IM 渠道 |
| 升级 | Escalation | KB 无法解决时自动创建工单 |