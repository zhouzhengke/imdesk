# Tech Stack Constraints

## 版本基准

所有依赖版本以 Spring Boot 3.x 和 Java 17 为基线，禁止使用已 EOL 的版本。

## 后端核心依赖

```
java.version              = 17
spring-boot.version       = 3.3.x
spring-ai.version         = 1.0.x (Milestone, 关注 GA 发布)
spring-statemachine.version = 4.0.x
mysql-connector.version   = 8.4.x
```

| 用途 | 技术 | 约束 |
|------|------|------|
| Web 框架 | Spring Boot 3.x + Spring MVC | RESTful API，统一 JSON 响应格式 |
| AI 集成 | Spring AI | 统一 LLM 调用抽象；通过 `spring.ai.openai` 或 `spring.ai.qianfan` 等 starter 接入；禁止直接在业务代码中调用裸 SDK |
| 状态机 | Spring StateMachine 4.x + Enum | 工单生命周期用状态机管理；禁止在 Service 中用 if-else 硬编码状态转移 |
| ORM | MyBatis-Plus 3.5.x | XML 复杂查询 + Lambda 简单查询；禁止拼接 SQL 字符串 |
| 数据库 | MySQL 8.0+ | InnoDB 引擎，utf8mb4 字符集；所有表必须有 `id`、`created_at`、`updated_at` |
| 缓存 | Redis 7.x + Spring Cache + Redisson | 会话缓存、在线状态、分布式锁；Key 命名：`{模块}:{实体}:{标识}` 如 `im:session:{sessionId}` |
| 消息队列 | RabbitMQ 3.13.x + Spring AMQP | IM 消息缓冲、通知异步发送；Exchange/Queue 命名前缀 `im.`；消费端必须做幂等处理 |
| 搜索引擎 | Elasticsearch 8.x | 知识库向量检索（dense_vector 类型）；版本号写入配置，不允许硬编码 |
| 定时任务 | XXL-JOB 2.4.x 或 Spring Scheduled | 超时检测、延期提醒、数据清理；分布式环境必须用 XXL-JOB |
| 配置中心 | Nacos 2.x 或 application.yml | Phase 1 可用 yml；预留 Nacos 接入接口 |

## 前端核心依赖

```
vue.version                = 3.4.x
element-plus.version       = 2.8.x
pinia.version              = 2.2.x
```

| 用途 | 技术 | 约束 |
|------|------|------|
| 框架 | Vue 3 Composition API | 使用 `<script setup lang="ts">` 语法；禁止 Options API |
| 类型 | TypeScript 5.x | 所有 .vue 和 .ts 文件必须通过 `vue-tsc --noEmit` |
| UI 组件库 | Element Plus | 统一使用 El- 前缀组件；禁止混用其他 UI 库 |
| 状态管理 | Pinia | 全局状态（用户信息、WebSocket 连接、未读数）放 Pinia；页面级状态用组件内 ref |
| 路由 | Vue Router 4.x | 路由配置集中管理；需要鉴权的路由统一加 meta.requiresAuth |
| HTTP 客户端 | Axios 1.x | 统一拦截器（Token 注入、错误处理、401 跳转） |
| 实时通信 | SockJS + STOMP over WebSocket | 连接 URL 从配置获取；断线自动重连（指数退避，最大 30s） |
| 构建 | Vite 5.x | 生产构建必须通过 `vite build`；禁止保留 console.log 到生产环境 |

## 禁止引入

以下技术在 Phase 1 明确不引入：

- ❌ 流程引擎（Flowable / Activiti / Camunda）— 工单用状态机即可
- ❌ 微服务框架（Spring Cloud / Dubbo）— Phase 1 单体部署，多实例即可
- ❌ 分库分表中间件（ShardingSphere）— Phase 1 数据量不需要
- ❌ MQ 其他选型（Kafka / RocketMQ / Pulsar）— RabbitMQ 足够
- ❌ GraphQL — 全部用 RESTful
- ❌ Next.js / Nuxt.js — 前端纯 Vue 3 SPA

## 依赖治理

- 新增依赖必须在 `pom.xml` 或 `package.json` 中锁定版本号，不允许使用 latest/rc/SNAPSHOT
- 第三方依赖引入前需评估：license 兼容性、维护活跃度、安全漏洞记录
- 禁止引入只使用其中 1-2 个工具方法的"巨型工具库"