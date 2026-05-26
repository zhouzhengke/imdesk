# IM工单系统开发计划

> 版本: v1.0 | 日期: 2026-05-26 | 基于设计文档: docs/superpowers/specs/2026-05-26-im-ticket-system-design.md

---

## 总览

### 项目结构

```
im-ticket/
├── im-ticket-common/       # 公共模块：枚举、异常、工具类、常量
├── im-ticket-dao/           # 数据访问层：Entity、Mapper、数据库初始化
├── im-ticket-infra/         # 基础设施层：渠道适配、AI、MQ、缓存、通知
├── im-ticket-service/       # 业务服务层：领域服务 + 应用服务
├── im-ticket-api/           # API 层：REST Controller
└── im-ticket-web/           # 前端：Vue 3 + Element Plus 工作台
```

### 开发分期

| 阶段 | 内容 | 预估工期 | 产出物 |
|------|------|---------|--------|
| Phase 0 | 项目初始化与基础设施 | 2 天 | 可运行的 Spring Boot 项目骨架 |
| Phase 1 | 数据库与 DAO 层 | 2 天 | 全部数据表 + Mapper 可用 |
| Phase 2 | 基础设施层 | 4 天 | 渠道适配器 + AI + MQ + 缓存 + 通知 |
| Phase 3 | 领域服务层 | 5 天 | 状态机 + 规则引擎 + 值班引擎 + 知识检索 |
| Phase 4 | 应用服务层 | 3 天 | 业务流程编排 |
| Phase 5 | API 层 + WebSocket | 2 天 | REST API + 实时通信 |
| Phase 6 | 前端工作台 | 5 天 | Vue 3 客服工作台可用 |
| Phase 7 | 集成测试与联调 | 3 天 | 核心 E2E 旅程通过 |
| **合计** | | **26 天** | |

---

## Phase 0: 项目初始化与基础设施（2 天）

### 任务 0.1: Maven 多模块骨架

**目标**: 创建可编译的多模块 Maven 项目，所有模块依赖正确

**创建文件**:

`im-ticket/pom.xml` — 父 POM:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.company</groupId>
    <artifactId>im-ticket</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>IM Ticket System</name>

    <modules>
        <module>im-ticket-common</module>
        <module>im-ticket-dao</module>
        <module>im-ticket-infra</module>
        <module>im-ticket-service</module>
        <module>im-ticket-api</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <!-- 依赖版本 -->
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <spring-ai.version>1.0.0-M4</spring-ai.version>
        <redisson.version>3.32.0</redisson.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>im-ticket-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>im-ticket-dao</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>im-ticket-infra</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>im-ticket-service</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- 数据库 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Spring AI -->
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
                <version>${spring-ai.version}</version>
            </dependency>

            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

各子模块 `pom.xml`:
- `im-ticket-common/pom.xml`: 继承 parent，依赖 lombok、jackson、slf4j、hutool-core、jakarta.validation
- `im-ticket-dao/pom.xml`: 继承 parent，依赖 common、mybatis-plus、mysql-connector
- `im-ticket-infra/pom.xml`: 继承 parent，依赖 common、dao、spring-boot-starter-amqp、spring-boot-starter-data-redis、redisson、spring-ai-openai、elasticsearch-java
- `im-ticket-service/pom.xml`: 继承 parent，依赖 common、dao、infra、spring-statemachine-core
- `im-ticket-api/pom.xml`: 继承 parent，依赖 common、service、spring-boot-starter-web、spring-boot-starter-websocket、spring-boot-starter-validation

### 任务 0.2: Spring Boot 启动类与基础配置

**目标**: 项目能启动并响应健康检查

`im-ticket-api/src/main/java/com/company/imticket/ImTicketApplication.java`:
```java
package com.company.imticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.company.imticket")
@MapperScan("com.company.imticket.dao.mapper")
public class ImTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImTicketApplication.class, args);
    }
}
```

`im-ticket-api/src/main/resources/application.yml`:
```yaml
server:
  port: 8082

spring:
  application:
    name: im-ticket
  profiles:
    active: dev
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/im_ticket?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-4o
          temperature: 0.1

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.company.imticket: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

`im-ticket-api/src/main/resources/application-dev.yml`:
```yaml
logging:
  level:
    com.company.imticket: DEBUG

im:
  channel:
    wecom:
      corp-id: ${WECOM_CORP_ID:}
      secret: ${WECOM_SECRET:}
      token: ${WECOM_TOKEN:}
      encoding-aes-key: ${WECOM_ENCODING_AES_KEY:}
    feishu:
      app-id: ${FEISHU_APP_ID:}
      app-secret: ${FEISHU_APP_SECRET:}
      verification-token: ${FEISHU_VERIFICATION_TOKEN:}
  duty:
    timeout-minutes: 5
  knowledge:
    escalation-rounds: 3
```

验证: 启动后 `curl http://localhost:8082/actuator/health` 返回 UP。

### 任务 0.3: Git 初始化与 .gitignore

`.gitignore`:
```
HELP.md
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

### Node ###
node_modules/
dist/
*.log
npm-debug.log*

### Superpowers ###
.superpowers/

### Environment ###
.env
.env.local
.env.*.local
*.log
```

---

## Phase 1: 数据库与 DAO 层（2 天）

### 任务 1.1: 数据库初始化 SQL

**目标**: 生成全部数据表，可导入 MySQL

`im-ticket-dao/src/main/resources/db/init.sql` — 核心结构：

```sql
CREATE DATABASE IF NOT EXISTS im_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE im_ticket;

-- 资方表
CREATE TABLE im_capital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '资方名称',
    contact_person VARCHAR(50) COMMENT '对接人',
    contact_phone VARCHAR(20) COMMENT '对接人电话',
    contract_start DATE COMMENT '合同开始日期',
    contract_end DATE COMMENT '合同结束日期',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0停用',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_name (name)
) ENGINE=InnoDB COMMENT='资方表';

-- 资方渠道映射表
CREATE TABLE im_capital_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    capital_id BIGINT NOT NULL COMMENT '资方ID',
    channel VARCHAR(20) NOT NULL COMMENT '渠道: wecom/feishu',
    identifier_type VARCHAR(30) NOT NULL COMMENT '识别类型: corp_tag/tenant_key/group_id/user_id',
    identifier_value VARCHAR(200) NOT NULL COMMENT '识别值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_capital (capital_id),
    INDEX idx_channel_identifier (channel, identifier_value)
) ENGINE=InnoDB COMMENT='资方渠道映射表';

-- 客服表
CREATE TABLE im_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
    password VARCHAR(200) NOT NULL COMMENT '加密密码',
    name VARCHAR(50) NOT NULL COMMENT '显示名称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机',
    role VARCHAR(20) NOT NULL DEFAULT 'AGENT' COMMENT '角色: AGENT/ADMIN/DUTY_MANAGER',
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态: ONLINE/OFFLINE/BREAK',
    wecom_user_id VARCHAR(100) COMMENT '企业微信UserId',
    feishu_open_id VARCHAR(100) COMMENT '飞书OpenId',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='客服表';

-- 工单表
CREATE TABLE im_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(30) NOT NULL UNIQUE COMMENT '工单编号 IM-YYYYMMDD-XXXX',
    capital_id BIGINT NOT NULL COMMENT '资方ID',
    channel VARCHAR(20) NOT NULL COMMENT '渠道',
    channel_user_id VARCHAR(200) NOT NULL COMMENT '渠道用户标识',
    channel_group_id VARCHAR(200) COMMENT '渠道群聊ID',
    user_name VARCHAR(100) COMMENT '用户昵称',
    description TEXT COMMENT '问题描述',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/TRANSFERRED/REJECTED/DEFERRED/RESOLVED/WAITING_CONFIRM/CLOSED',
    priority VARCHAR(10) NOT NULL DEFAULT 'normal' COMMENT 'normal/high/urgent',
    category VARCHAR(50) COMMENT '分类',
    tags VARCHAR(500) COMMENT '标签 JSON数组',
    assigned_agent_id BIGINT COMMENT '负责客服ID',
    context_summary TEXT COMMENT '会话摘要(LLM生成)',
    resolved_at DATETIME COMMENT '解决时间',
    closed_at DATETIME COMMENT '关闭时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_ticket_no (ticket_no),
    INDEX idx_capital (capital_id),
    INDEX idx_status_agent (status, assigned_agent_id),
    INDEX idx_channel_created (channel, created_at)
) ENGINE=InnoDB COMMENT='工单表';

-- 工单消息表
CREATE TABLE im_ticket_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    sender_type VARCHAR(10) NOT NULL COMMENT 'USER/AGENT/SYSTEM',
    sender_id VARCHAR(100) COMMENT '发送人ID',
    sender_name VARCHAR(100) COMMENT '发送人名称',
    content TEXT NOT NULL COMMENT '消息内容',
    content_type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/file',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket (ticket_id, created_at)
) ENGINE=InnoDB COMMENT='工单消息表';

-- 工单状态流水表
CREATE TABLE im_ticket_state_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    from_status VARCHAR(30) COMMENT '原状态',
    to_status VARCHAR(30) NOT NULL COMMENT '目标状态',
    operator_id VARCHAR(100) COMMENT '操作人ID',
    operator_type VARCHAR(10) COMMENT 'USER/AGENT/SYSTEM',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket (ticket_id, created_at)
) ENGINE=InnoDB COMMENT='工单状态流水表';

-- 值班班次表
CREATE TABLE im_shift (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '班次名称',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    duty_type VARCHAR(20) NOT NULL DEFAULT 'weekday' COMMENT 'weekday/weekend',
    primary_agent_ids VARCHAR(500) COMMENT '主值班JSON数组',
    backup_agent_ids VARCHAR(500) COMMENT '备选值班JSON数组',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB COMMENT='值班班次表';

-- 排班表
CREATE TABLE im_shift_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_date DATE NOT NULL COMMENT '排班日期',
    shift_id BIGINT NOT NULL COMMENT '班次ID',
    agent_ids VARCHAR(500) COMMENT '实际值班人员JSON数组(可覆盖班次默认)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_date_shift (schedule_date, shift_id)
) ENGINE=InnoDB COMMENT='排班表';

-- 通知模板表
CREATE TABLE im_notification_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    direction VARCHAR(10) NOT NULL COMMENT 'INTERNAL/EXTERNAL',
    channel VARCHAR(20) NOT NULL COMMENT 'workbench/wecom/feishu',
    format VARCHAR(10) NOT NULL DEFAULT 'text' COMMENT 'text/markdown',
    title VARCHAR(200) COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '模板内容 含{{变量}}',
    enabled TINYINT DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_code (code)
) ENGINE=InnoDB COMMENT='通知模板表';

-- FAQ知识库表
CREATE TABLE im_knowledge_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(500) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    keywords VARCHAR(300) COMMENT '关键词 逗号分隔',
    category VARCHAR(50) COMMENT '分类',
    hit_count BIGINT DEFAULT 0 COMMENT '命中次数',
    enabled TINYINT DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FULLTEXT INDEX ft_question_keywords (question, keywords)
) ENGINE=InnoDB COMMENT='FAQ知识库表';

-- 知识文档表
CREATE TABLE im_knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT 'pdf/word/markdown/txt',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小 字节',
    chunk_count INT COMMENT '切片数',
    es_index_name VARCHAR(100) COMMENT 'ES索引名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_title (title)
) ENGINE=InnoDB COMMENT='知识文档表';

-- 历史工单归档表
CREATE TABLE im_knowledge_ticket_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT '原工单ID',
    ticket_no VARCHAR(30) NOT NULL COMMENT '工单编号',
    capital_name VARCHAR(100) COMMENT '资方名称',
    summary TEXT COMMENT '问题摘要(LLM生成)',
    solution TEXT COMMENT '解决方案(LLM生成)',
    keywords VARCHAR(300) COMMENT '关键词',
    category VARCHAR(50) COMMENT '分类',
    es_doc_id VARCHAR(100) COMMENT 'ES文档ID',
    reviewed_by BIGINT COMMENT '审核人ID',
    reviewed_at DATETIME COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket_no (ticket_no),
    INDEX idx_keywords (keywords),
    FULLTEXT INDEX ft_summary_solution (summary, solution)
) ENGINE=InnoDB COMMENT='历史工单归档表';

-- 消息失败队列表
CREATE TABLE im_message_fail_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT COMMENT '工单ID',
    channel VARCHAR(20) NOT NULL,
    channel_user_id VARCHAR(200),
    channel_group_id VARCHAR(200),
    content TEXT NOT NULL,
    content_type VARCHAR(20) DEFAULT 'text',
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 5,
    next_retry_at DATETIME,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/RETRYING/FAILED/SUCCESS',
    error_msg VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_next (status, next_retry_at)
) ENGINE=InnoDB COMMENT='消息失败队列表';
```

### 任务 1.2: Entity 类

**目标**: 每个表对应一个 Entity，使用 MyBatis-Plus 注解

创建文件（均在 `im-ticket-dao/src/main/java/com/company/imticket/dao/entity/`）:

- `Capital.java` — 对应 `im_capital`
- `CapitalChannelMapping.java` — 对应 `im_capital_channel`
- `Agent.java` — 对应 `im_agent`
- `Ticket.java` — 对应 `im_ticket`
- `TicketMessage.java` — 对应 `im_ticket_message`
- `TicketStateLog.java` — 对应 `im_ticket_state_log`
- `Shift.java` — 对应 `im_shift`
- `ShiftSchedule.java` — 对应 `im_shift_schedule`
- `NotificationTemplate.java` — 对应 `im_notification_template`
- `KnowledgeFaq.java` — 对应 `im_knowledge_faq`
- `KnowledgeDocument.java` — 对应 `im_knowledge_document`
- `KnowledgeTicketArchive.java` — 对应 `im_knowledge_ticket_archive`
- `MessageFailQueue.java` — 对应 `im_message_fail_queue`

Entity 基类 `BaseEntity.java`:
```java
package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

示例 `Ticket.java`:
```java
package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_ticket")
public class Ticket extends BaseEntity {
    private String ticketNo;
    private Long capitalId;
    private String channel;
    private String channelUserId;
    private String channelGroupId;
    private String userName;
    private String description;
    private String status;
    private String priority;
    private String category;
    private String tags;
    private Long assignedAgentId;
    private String contextSummary;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}
```

### 任务 1.3: Mapper 接口

**目标**: 每个 Entity 对应一个 Mapper 接口

创建文件（均在 `im-ticket-dao/src/main/java/com/company/imticket/dao/mapper/`）:

`TicketMapper.java`:
```java
package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.imticket.dao.entity.Ticket;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface TicketMapper extends BaseMapper<Ticket> {

    IPage<Ticket> selectPageWithFilters(Page<Ticket> page,
        @Param("channel") String channel,
        @Param("capitalId") Long capitalId,
        @Param("status") String status,
        @Param("agentId") Long agentId,
        @Param("keyword") String keyword);

    @Select("SELECT * FROM im_ticket WHERE channel_user_id = #{channelUserId} " +
            "AND channel = #{channel} AND status NOT IN ('CLOSED') LIMIT 1")
    Ticket findOpenTicketByChannelUser(@Param("channel") String channel,
                                       @Param("channelUserId") String channelUserId);

    @Select("SELECT COUNT(*) FROM im_ticket WHERE status = 'PENDING' AND deleted = 0")
    Long countPendingTickets();
}
```

对应的 XML Mapper `im-ticket-dao/src/main/resources/mapper/TicketMapper.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.company.imticket.dao.mapper.TicketMapper">

    <select id="selectPageWithFilters" resultType="com.company.imticket.dao.entity.Ticket">
        SELECT * FROM im_ticket
        WHERE deleted = 0
        <if test="channel != null and channel != ''">
            AND channel = #{channel}
        </if>
        <if test="capitalId != null">
            AND capital_id = #{capitalId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="agentId != null">
            AND assigned_agent_id = #{agentId}
        </if>
        <if test="keyword != null and keyword != ''">
            AND (ticket_no LIKE CONCAT('%', #{keyword}, '%')
                 OR description LIKE CONCAT('%', #{keyword}, '%')
                 OR user_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY created_at DESC
    </select>
</mapper>
```

同样方式创建: `CapitalMapper.java`、`AgentMapper.java`、`TicketMessageMapper.java`、`TicketStateLogMapper.java`、`ShiftMapper.java`、`ShiftScheduleMapper.java`、`NotificationTemplateMapper.java`、`KnowledgeFaqMapper.java`、`KnowledgeDocumentMapper.java`、`KnowledgeTicketArchiveMapper.java`、`MessageFailQueueMapper.java`

### 任务 1.4: MyBatis-Plus 配置

`im-ticket-dao/src/main/java/com/company/imticket/dao/config/MybatisPlusConfig.java`:
```java
package com.company.imticket.dao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

自动填充处理器 `MetaObjectHandlerConfig.java`:
```java
package com.company.imticket.dao.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

## Phase 2: 基础设施层（4 天）

### 任务 2.1: Common 模块 — 枚举与异常

**目标**: 全局可用的枚举和异常类

**文件清单**:

`im-ticket-common/src/main/java/com/company/imticket/common/enums/TicketStatus.java`:
```java
package com.company.imticket.common.enums;

import lombok.Getter;
import java.util.Set;

@Getter
public enum TicketStatus {
    PENDING("待处理"),
    IN_PROGRESS("处理中"),
    TRANSFERRED("已转交"),
    REJECTED("已驳回"),
    DEFERRED("已延期"),
    RESOLVED("已解决"),
    WAITING_CONFIRM("待确认"),
    CLOSED("已关闭");

    private final String label;

    TicketStatus(String label) { this.label = label; }

    public boolean isTerminal() { return this == CLOSED; }

    public boolean canTransition(TicketStatus target) {
        return switch (this) {
            case PENDING -> Set.of(IN_PROGRESS).contains(target);
            case IN_PROGRESS -> Set.of(RESOLVED, TRANSFERRED, REJECTED, DEFERRED).contains(target);
            case TRANSFERRED -> Set.of(IN_PROGRESS, PENDING).contains(target);
            case REJECTED -> Set.of(PENDING).contains(target);
            case DEFERRED -> Set.of(IN_PROGRESS).contains(target);
            case RESOLVED -> Set.of(WAITING_CONFIRM, CLOSED).contains(target);
            case WAITING_CONFIRM -> Set.of(CLOSED, IN_PROGRESS).contains(target);
            case CLOSED -> false;
        };
    }
}
```

`im-ticket-common/src/main/java/com/company/imticket/common/enums/ChannelType.java`:
```java
package com.company.imticket.common.enums;

public enum ChannelType {
    WECOM("wecom", "企业微信"),
    FEISHU("feishu", "飞书");

    private final String code;
    private final String label;
    ChannelType(String code, String label) { this.code = code; this.label = label; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
```

`im-ticket-common/src/main/java/com/company/imticket/common/enums/RoutingIntent.java`:
```java
package com.company.imticket.common.enums;

public enum RoutingIntent {
    TICKET_QUERY("工单查询"),
    CREATE_TICKET("创建工单"),
    SUPPLEMENT("补充说明"),
    KNOWLEDGE_QUERY("知识库问答"),
    BUSINESS_QUERY("业务查询"),
    CHITCHAT("寒暄"),
    URGENT_ESCALATION("紧急升级");

    private final String label;
    RoutingIntent(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

`im-ticket-common/src/main/java/com/company/imticket/common/exception/BizErrorCode.java`:
```java
package com.company.imticket.common.exception;

import lombok.Getter;

@Getter
public enum BizErrorCode {
    SUCCESS(0, "success"),
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或Token已过期"),
    FORBIDDEN(40300, "无操作权限"),
    NOT_FOUND(40400, "资源不存在"),
    INTERNAL_ERROR(50000, "系统内部错误"),

    // 工单 10xxx
    TICKET_NOT_FOUND(10001, "工单不存在"),
    TICKET_STATUS_ILLEGAL(10002, "当前工单状态不允许此操作"),
    TICKET_ALREADY_ASSIGNED(10003, "工单已被其他客服领取"),
    TICKET_HAS_OPEN(10004, "该会话已存在未关闭工单"),

    // 路由 11xxx
    ROUTING_LLM_TIMEOUT(11001, "大模型路由超时"),
    ROUTING_LLM_ERROR(11002, "大模型路由服务异常"),

    // 知识库 12xxx
    KB_FAQ_DUPLICATE(12001, "FAQ问题已存在"),
    KB_DOC_PARSE_ERROR(12002, "文档解析失败"),
    KB_SEARCH_TIMEOUT(12004, "知识库搜索超时"),

    // 渠道 13xxx
    CHANNEL_SIGNATURE_INVALID(13001, "渠道回调签名验证失败"),
    CHANNEL_SEND_FAILED(13002, "渠道消息发送失败"),
    CHANNEL_UNSUPPORTED(13003, "不支持的渠道类型"),

    // 值班 14xxx
    DUTY_NO_AGENT_AVAILABLE(14001, "当前无可用的值班客服"),
    DUTY_SHIFT_CONFLICT(14002, "排班时间冲突"),

    // 通知 15xxx
    NOTIFICATION_TEMPLATE_NOT_FOUND(15001, "通知模板不存在"),
    NOTIFICATION_SEND_FAILED(15002, "通知发送失败");

    private final int code;
    private final String message;
    BizErrorCode(int code, String message) { this.code = code; this.message = message; }
}
```

`im-ticket-common/src/main/java/com/company/imticket/common/exception/BizException.java`:
```java
package com.company.imticket.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final BizErrorCode errorCode;
    private final String detail;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BizException(BizErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " - " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
```

以及子类异常: `TicketException.java`、`RoutingException.java`、`ChannelException.java`、`DutyException.java` — 均继承 `BizException`。

`im-ticket-common/src/main/java/com/company/imticket/common/dto/R.java`:
```java
package com.company.imticket.common.dto;

import com.company.imticket.common.exception.BizErrorCode;
import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = BizErrorCode.SUCCESS.getCode();
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(BizErrorCode error) {
        R<T> r = new R<>();
        r.code = error.getCode();
        r.message = error.getMessage();
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

`im-ticket-common/src/main/java/com/company/imticket/common/dto/PageResp.java`:
```java
package com.company.imticket.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResp<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
}
```

### 任务 2.2: Channel Adapter 抽象与实现

**目标**: 统一的渠道接入接口，企业微信和飞书各一个适配器

`im-ticket-infra/src/main/java/com/company/imticket/infra/channel/ChannelMessage.java`:
```java
package com.company.imticket.infra.channel;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ChannelMessage {
    private String messageId;
    private String channel;
    private String channelUserId;
    private String channelGroupId;
    private String userName;
    private String content;
    private String contentType;
    private LocalDateTime timestamp;
    private Map<String, Object> rawPayload;
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/channel/CapitalIdentity.java`:
```java
package com.company.imticket.infra.channel;

import lombok.Data;

@Data
public class CapitalIdentity {
    private Long capitalId;
    private String capitalName;
    private String channel;
    private String identifierType;
    private String identifierValue;
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/channel/ChannelAdapter.java`:
```java
package com.company.imticket.infra.channel;

import com.company.imticket.common.enums.ChannelType;

public interface ChannelAdapter {
    ChannelType getType();
    ChannelMessage normalize(Map<String, Object> rawPayload);
    void sendMessage(String channelUserId, String channelGroupId, String contentType, String content);
    CapitalIdentity resolveCapital(Map<String, Object> rawPayload);
    boolean verifySignature(String signature, String timestamp, String nonce, String body);
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/channel/WeComChannelAdapter.java`:
```java
package com.company.imticket.infra.channel;

import com.company.imticket.common.enums.ChannelType;
import com.company.imticket.dao.entity.CapitalChannelMapping;
import com.company.imticket.dao.mapper.CapitalChannelMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeComChannelAdapter implements ChannelAdapter {

    private final CapitalChannelMappingMapper capitalChannelMappingMapper;

    @Override
    public ChannelType getType() { return ChannelType.WECOM; }

    @Override
    public ChannelMessage normalize(Map<String, Object> rawPayload) {
        // 从企微回调 JSON 提取统一格式消息
        ChannelMessage msg = new ChannelMessage();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setChannel("wecom");
        msg.setChannelUserId((String) rawPayload.getOrDefault("FromUserName", ""));
        // 企微单聊消息的群ID即为成员ID，群聊消息有独立的 ChatId
        msg.setChannelGroupId((String) rawPayload.getOrDefault("ChatId", msg.getChannelUserId()));
        msg.setContent((String) rawPayload.getOrDefault("Content", ""));
        msg.setContentType((String) rawPayload.getOrDefault("MsgType", "text"));
        msg.setTimestamp(java.time.LocalDateTime.now());
        msg.setRawPayload(rawPayload);
        return msg;
    }

    @Override
    public void sendMessage(String channelUserId, String channelGroupId, String contentType, String content) {
        // TODO: 调用企微「发送应用消息」API
        // POST https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=TOKEN
        // 群聊用 ChatId，单聊用 UserId
        log.info("WeCom sendMessage: to={}, group={}, type={}, content={}",
                channelUserId, channelGroupId, contentType, content);
    }

    @Override
    public CapitalIdentity resolveCapital(Map<String, Object> rawPayload) {
        String externalUserId = (String) rawPayload.getOrDefault("ExternalUserId", "");
        // 通过外部联系人标签或 CorpId 查询资方映射
        CapitalChannelMapping mapping = capitalChannelMappingMapper
                .findByChannelAndIdentifier("wecom", externalUserId);
        if (mapping == null) {
            log.warn("未识别的企微用户: {}", externalUserId);
            return null;
        }
        CapitalIdentity identity = new CapitalIdentity();
        identity.setCapitalId(mapping.getCapitalId());
        identity.setChannel("wecom");
        identity.setIdentifierType(mapping.getIdentifierType());
        identity.setIdentifierValue(mapping.getIdentifierValue());
        return identity;
    }

    @Override
    public boolean verifySignature(String signature, String timestamp, String nonce, String body) {
        // TODO: 企微回调验签逻辑
        // SHA1(sort(token, timestamp, nonce, body))
        return true; // Phase 1 先通过，接入时实现完整验签
    }
}
```

`FeishuChannelAdapter.java` 同理，基于飞书开放平台回调结构实现。

### 任务 2.3: AI 集成层

**目标**: 通过 Spring AI 调用大模型，实现意图识别和摘要生成

`im-ticket-infra/src/main/java/com/company/imticket/infra/ai/AiClient.java`:
```java
package com.company.imticket.infra.ai;

import com.company.imticket.infra.ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final ChatClient chatClient;

    /**
     * 意图识别：分析用户消息，返回结构化意图
     */
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

        // 解析 JSON 为 RoutingResult
        return parseRoutingResult(response);
    }

    /**
     * 生成工单摘要
     */
    public String generateTicketSummary(String conversationText) {
        String prompt = "请用一句话总结以下工单对话的核心问题和解决方案（100字以内）：\n" + conversationText;
        return chatClient.prompt().user(prompt).call().content();
    }

    /**
     * 生成知识库回答
     */
    public String generateKnowledgeAnswer(String question, String context) {
        String systemPrompt = "你是一个客服助手。根据提供的参考信息回答用户问题。如果信息不足以回答，请明确说明。";
        String userPrompt = String.format("参考信息：\n%s\n\n用户问题：%s", context, question);
        return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
    }

    private RoutingResult parseRoutingResult(String json) {
        // 使用 Jackson/Gson 解析 JSON
        // 解析失败则降级为 create_ticket
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, RoutingResult.class);
        } catch (Exception e) {
            log.error("Failed to parse LLM routing result: {}", json, e);
            return RoutingResult.fallback();
        }
    }
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/ai/dto/RoutingResult.java`:
```java
package com.company.imticket.infra.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoutingResult {
    private String intent;
    private double confidence;
    private Map<String, Object> entities;
    private String priority;
    private String sentiment;

    public static RoutingResult fallback() {
        RoutingResult r = new RoutingResult();
        r.intent = "create_ticket";
        r.confidence = 0.0;
        r.priority = "normal";
        r.sentiment = "neutral";
        return r;
    }
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/ai/dto/IntentRecognitionContext.java`:
```java
package com.company.imticket.infra.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntentRecognitionContext {
    private String capitalName;
    private String userName;
    private boolean hasOpenTicket;
    private int kbInteractionCount;
    private String message;
}
```

### 任务 2.4: RabbitMQ 配置

**目标**: 声明消息队列、交换机、绑定关系

`im-ticket-infra/src/main/java/com/company/imticket/infra/mq/MqConstants.java`:
```java
package com.company.imticket.infra.mq;

public class MqConstants {
    public static final String EXCHANGE_IM = "im.topic";
    public static final String QUEUE_MESSAGE_IN = "im.message.in";
    public static final String QUEUE_NOTIFICATION = "im.notification";
    public static final String QUEUE_MESSAGE_DLX = "im.message.dlx";
    public static final String ROUTING_KEY_MESSAGE = "im.message.*";
    public static final String ROUTING_KEY_NOTIFICATION = "im.notification.*";
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/mq/RabbitMqConfig.java`:
```java
package com.company.imticket.infra.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange imExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_IM);
    }

    @Bean
    public Queue messageInQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_MESSAGE_IN)
                .deadLetterExchange(MqConstants.EXCHANGE_IM)
                .deadLetterRoutingKey("im.message.dlx")
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_NOTIFICATION).build();
    }

    @Bean
    public Queue messageDlxQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_MESSAGE_DLX).build();
    }

    @Bean
    public Binding messageInBinding() {
        return BindingBuilder.bind(messageInQueue())
                .to(imExchange()).with(MqConstants.ROUTING_KEY_MESSAGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(imExchange()).with(MqConstants.ROUTING_KEY_NOTIFICATION);
    }

    @Bean
    public Binding messageDlxBinding() {
        return BindingBuilder.bind(messageDlxQueue())
                .to(imExchange()).with("im.message.dlx");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
```

### 任务 2.5: Redis 与缓存配置

**目标**: Redis 连接、分布式锁、会话缓存

`im-ticket-infra/src/main/java/com/company/imticket/infra/cache/CacheConstants.java`:
```java
package com.company.imticket.infra.cache;

public class CacheConstants {
    public static final String SESSION_PREFIX = "im:session:";       // 会话缓存
    public static final String AGENT_STATUS = "im:agent:status:";    // 客服在线状态
    public static final String LOCK_ASSIGN = "im:lock:assign:";      // 分配锁
    public static final String KB_HIT_COUNT = "im:kb:hit:";          // KB命中计数
}
```

`im-ticket-infra/src/main/java/com/company/imticket/infra/cache/SessionCacheService.java`:
```java
package com.company.imticket.infra.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SessionCacheService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 记录会话中 KB 交互轮次
     */
    public int incrementKbRound(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":kb_rounds";
        Long rounds = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        return rounds != null ? rounds.intValue() : 0;
    }

    /**
     * 重置会话的 KB 交互轮次（工单创建后重置）
     */
    public void resetKbRound(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":kb_rounds";
        redisTemplate.delete(key);
    }

    /**
     * 缓存上次处理该会话的客服ID
     */
    public void cacheLastAgent(String channel, String userId, String groupId, Long agentId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":last_agent";
        redisTemplate.opsForValue().set(key, String.valueOf(agentId), 7, TimeUnit.DAYS);
    }

    public Long getLastAgent(String channel, String userId, String groupId) {
        String key = CacheConstants.SESSION_PREFIX + channel + ":" + userId + ":" + groupId + ":last_agent";
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.valueOf(val) : null;
    }
}
```

### 任务 2.6: 通知发送基础设施

**目标**: 模板渲染 + 多渠道通知发送

`im-ticket-infra/src/main/java/com/company/imticket/infra/notification/NotificationSender.java`:
```java
package com.company.imticket.infra.notification;

import com.company.imticket.dao.entity.NotificationTemplate;
import com.company.imticket.dao.mapper.NotificationTemplateMapper;
import com.company.imticket.infra.channel.ChannelAdapter;
import com.company.imticket.infra.mq.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationTemplateMapper templateMapper;
    private final List<ChannelAdapter> channelAdapters;
    private final RabbitTemplate rabbitTemplate;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /**
     * 根据模板编码发送通知
     */
    public void sendByTemplate(String templateCode, Map<String, String> variables,
                                String targetUserId, String targetGroupId) {
        NotificationTemplate template = templateMapper.findByCode(templateCode);
        if (template == null) {
            log.error("通知模板不存在: {}", templateCode);
            return;
        }

        String content = renderTemplate(template.getContent(), variables);
        sendAsync(template, content, targetUserId, targetGroupId);
    }

    /**
     * 渲染模板变量
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 异步发送通知（通过 MQ 解耦）
     */
    private void sendAsync(NotificationTemplate template, String content,
                           String targetUserId, String targetGroupId) {
        NotificationMessage msg = new NotificationMessage();
        msg.setTemplateCode(template.getCode());
        msg.setChannel(template.getChannel());
        msg.setContent(content);
        msg.setTargetUserId(targetUserId);
        msg.setTargetGroupId(targetGroupId);
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_IM,
                MqConstants.ROUTING_KEY_NOTIFICATION, msg);
    }
}
```

---

## Phase 3: 领域服务层（5 天）

### 任务 3.1: 工单状态机

**目标**: 严格的状态转移控制

`im-ticket-service/src/main/java/com/company/imticket/service/domain/ticket/TicketStateMachine.java`:
```java
package com.company.imticket.service.domain.ticket;

import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.common.exception.TicketException;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketStateLog;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketStateLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStateMachine {

    private final TicketMapper ticketMapper;
    private final TicketStateLogMapper stateLogMapper;

    @Transactional
    public Ticket transition(Ticket ticket, TicketStatus target, String operatorId, String operatorType, String remark) {
        TicketStatus current = TicketStatus.valueOf(ticket.getStatus());

        if (!current.canTransition(target)) {
            throw new TicketException(BizErrorCode.TICKET_STATUS_ILLEGAL,
                String.format("ticket=%s, %s → %s 不允许", ticket.getTicketNo(), current, target));
        }

        String fromStatus = ticket.getStatus();
        ticket.setStatus(target.name());
        applySideEffects(ticket, target);
        ticketMapper.updateById(ticket);

        // 记录状态流水
        TicketStateLog log = new TicketStateLog();
        log.setTicketId(ticket.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(target.name());
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setRemark(remark);
        stateLogMapper.insert(log);

        log.info("ticket state transition: {} {} → {}, operator={}",
                ticket.getTicketNo(), fromStatus, target.name(), operatorId);
        return ticket;
    }

    private void applySideEffects(Ticket ticket, TicketStatus target) {
        switch (target) {
            case IN_PROGRESS:
                if (ticket.getAssignedAgentId() == null) {
                    throw new TicketException(BizErrorCode.PARAM_INVALID, "领取工单必须指定客服");
                }
                break;
            case RESOLVED:
                ticket.setResolvedAt(LocalDateTime.now());
                break;
            case CLOSED:
                ticket.setClosedAt(LocalDateTime.now());
                break;
            case REJECTED:
                ticket.setAssignedAgentId(null);  // 清除分配，回到待分配池
                break;
        }
    }
}
```

### 任务 3.2: 规则引擎

**目标**: 前缀/关键词/正则匹配，毫秒级路由

`im-ticket-service/src/main/java/com/company/imticket/service/domain/routing/RuleEngineService.java`:
```java
package com.company.imticket.service.domain.routing;

import com.company.imticket.common.enums.RoutingIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RuleEngineService {

    // 前缀规则：{前缀, 对应意图}
    private static final Map<String, RoutingIntent> PREFIX_RULES = Map.of(
        "#查单", RoutingIntent.TICKET_QUERY,
        "#我的工单", RoutingIntent.TICKET_QUERY,
        "#帮助", RoutingIntent.CHITCHAT,
        "#?", RoutingIntent.CHITCHAT,
        "?", RoutingIntent.CHITCHAT,
        "帮助", RoutingIntent.CHITCHAT
    );

    // 关键词规则：{关键词, 意图}
    private static final Map<String, RoutingIntent> KEYWORD_RULES = Map.of(
        "转人工", RoutingIntent.CREATE_TICKET,
        "联系客服", RoutingIntent.CREATE_TICKET,
        "创建工单", RoutingIntent.CREATE_TICKET,
        "人工客服", RoutingIntent.CREATE_TICKET
    );

    // 工单编号正则
    private static final Pattern TICKET_NO_PATTERN =
        Pattern.compile("IM-\\d{8}-\\d{4}", Pattern.CASE_INSENSITIVE);

    /**
     * 规则匹配。命中返回意图，未命中返回 null。
     */
    public RuleMatchResult match(String message) {
        // 1. 前缀匹配
        for (Map.Entry<String, RoutingIntent> entry : PREFIX_RULES.entrySet()) {
            if (message.startsWith(entry.getKey())) {
                String cleanedMessage = message.substring(entry.getKey().length()).trim();
                return new RuleMatchResult(entry.getValue(), cleanedMessage, "prefix:" + entry.getKey());
            }
        }

        // 2. 工单编号匹配
        java.util.regex.Matcher ticketMatcher = TICKET_NO_PATTERN.matcher(message);
        if (ticketMatcher.find()) {
            return new RuleMatchResult(RoutingIntent.TICKET_QUERY, ticketMatcher.group(), "pattern:ticket_no");
        }

        // 3. 关键词匹配
        for (Map.Entry<String, RoutingIntent> entry : KEYWORD_RULES.entrySet()) {
            if (message.contains(entry.getKey())) {
                return new RuleMatchResult(entry.getValue(), message, "keyword:" + entry.getKey());
            }
        }

        return null; // 未命中
    }
}
```

`im-ticket-service/src/main/java/com/company/imticket/service/domain/routing/RuleMatchResult.java`:
```java
package com.company.imticket.service.domain.routing;

import com.company.imticket.common.enums.RoutingIntent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleMatchResult {
    private RoutingIntent intent;
    private String extractedParam;  // 提取的参数（如工单号）
    private String matchRule;       // 匹配的规则描述
}
```

### 任务 3.3: 值班分配引擎

**目标**: 5 级分配策略：上次客服 → 主值班 → 备选值班 → 待分配池 → 告警

`im-ticket-service/src/main/java/com/company/imticket/service/domain/duty/DutyAssignmentService.java`:
```java
package com.company.imticket.service.domain.duty;

import com.company.imticket.dao.entity.Agent;
import com.company.imticket.dao.mapper.AgentMapper;
import com.company.imticket.dao.mapper.ShiftMapper;
import com.company.imticket.dao.mapper.ShiftScheduleMapper;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.infra.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DutyAssignmentService {

    private final AgentMapper agentMapper;
    private final ShiftMapper shiftMapper;
    private final ShiftScheduleMapper scheduleMapper;
    private final SessionCacheService sessionCache;
    private final NotificationSender notificationSender;

    /**
     * 按优先级分配客服。返回分配结果，失败返回 null。
     */
    public AssignmentResult assign(String channel, String channelUserId,
                                    String channelGroupId, String capitalName) {
        // 策略1: 上下文继承 — 上次处理该会话的客服
        Long lastAgentId = sessionCache.getLastAgent(channel, channelUserId, channelGroupId);
        if (lastAgentId != null) {
            Agent lastAgent = agentMapper.selectById(lastAgentId);
            if (lastAgent != null && "ONLINE".equals(lastAgent.getStatus())) {
                log.info("assign by context inheritance: agent={}", lastAgentId);
                return new AssignmentResult(lastAgentId, lastAgent.getName(), "context_inheritance");
            }
        }

        // 策略2: 当前主值班
        List<Long> dutyAgentIds = getCurrentDutyAgents();
        for (Long agentId : dutyAgentIds) {
            Agent agent = agentMapper.selectById(agentId);
            if (agent != null && "ONLINE".equals(agent.getStatus())) {
                log.info("assign to primary duty: agent={}", agentId);
                return new AssignmentResult(agentId, agent.getName(), "primary_duty");
            }
        }

        // 策略3: 备选值班
        List<Long> backupAgentIds = getCurrentBackupAgents();
        for (Long agentId : backupAgentIds) {
            Agent agent = agentMapper.selectById(agentId);
            if (agent != null && "ONLINE".equals(agent.getStatus())) {
                log.info("assign to backup duty: agent={}", agentId);
                return new AssignmentResult(agentId, agent.getName(), "backup_duty");
            }
        }

        // 策略4: 无人在线 — 返回 null 进入待分配池
        log.warn("no available agent for: capital={}, channel={}", capitalName, channel);
        // 策略5: 发送管理员群告警
        notificationSender.sendByTemplate("duty_no_agent_alert",
                Map.of("capital_name", capitalName, "channel", channel), null, null);
        return null;
    }

    private List<Long> getCurrentDutyAgents() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // 查询今天当前时段的排班，返回主值班人员列表
        return scheduleMapper.findCurrentDutyAgents(today, now);
    }

    private List<Long> getCurrentBackupAgents() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return scheduleMapper.findCurrentBackupAgents(today, now);
    }
}
```

`im-ticket-service/src/main/java/com/company/imticket/service/domain/duty/AssignmentResult.java`:
```java
package com.company.imticket.service.domain.duty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignmentResult {
    private Long agentId;
    private String agentName;
    private String strategy;  // context_inheritance / primary_duty / backup_duty
}
```

### 任务 3.4: 知识库检索服务

**目标**: 三层检索 — FAQ → 文档 RAG → 历史工单

`im-ticket-service/src/main/java/com/company/imticket/service/domain/knowledge/KnowledgeSearchService.java`:
```java
package com.company.imticket.service.domain.knowledge;

import com.company.imticket.dao.entity.KnowledgeFaq;
import com.company.imticket.dao.mapper.KnowledgeFaqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchService {

    private final KnowledgeFaqMapper faqMapper;
    private final DocumentSearchService documentSearchService;
    private final TicketArchiveSearchService archiveSearchService;

    /**
     * 三层检索。返回结果，未命中返回 null。
     */
    public KnowledgeSearchResult search(String question) {
        // L1: FAQ 精确/模糊匹配
        List<KnowledgeFaq> faqResults = faqMapper.searchByQuestion(question);
        if (!faqResults.isEmpty()) {
            KnowledgeFaq hit = faqResults.get(0);
            faqMapper.incrementHitCount(hit.getId());
            log.info("KB L1 FAQ hit: question={}, faqId={}", question, hit.getId());
            return new KnowledgeSearchResult(
                hit.getAnswer(),
                "L1_FAQ",
                List.of(hit.getQuestion()),
                1.0
            );
        }

        // L2: 文档 RAG 检索
        KnowledgeSearchResult docResult = documentSearchService.search(question);
        if (docResult != null) {
            log.info("KB L2 document hit: question={}", question);
            return docResult;
        }

        // L3: 历史工单检索
        KnowledgeSearchResult archiveResult = archiveSearchService.search(question);
        if (archiveResult != null) {
            log.info("KB L3 archive hit: question={}", question);
            return archiveResult;
        }

        log.info("KB all layers missed: question={}", question);
        return null;
    }
}
```

`im-ticket-service/src/main/java/com/company/imticket/service/domain/knowledge/KnowledgeSearchResult.java`:
```java
package com.company.imticket.service.domain.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class KnowledgeSearchResult {
    private String answer;
    private String source;      // L1_FAQ / L2_DOC / L3_ARCHIVE
    private List<String> references;
    private double confidence;
}
```

`DocumentSearchService.java` 和 `TicketArchiveSearchService.java` — Phase 1 使用 MySQL FULLTEXT 索引做文本检索，预留 ES 向量检索接口。

### 任务 3.5: 工单编号生成器

`im-ticket-service/src/main/java/com/company/imticket/service/domain/ticket/TicketNoGenerator.java`:
```java
package com.company.imticket.service.domain.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TicketNoGenerator {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String key = "im:ticket:seq:" + datePart;
        Long seq = redisTemplate.opsForValue().increment(key);
        return String.format("IM-%s-%04d", datePart, seq != null ? seq : 1);
    }
}
```

---

## Phase 4: 应用服务层（3 天）

### 任务 4.1: 路由应用服务

**目标**: 编排规则引擎 + LLM + 知识库的完整路由流程

`im-ticket-service/src/main/java/com/company/imticket/service/application/routing/RoutingAppService.java`:
```java
package com.company.imticket.service.application.routing;

import com.company.imticket.common.enums.RoutingIntent;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.ai.dto.IntentRecognitionContext;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.service.domain.routing.RuleEngineService;
import com.company.imticket.service.domain.routing.RuleMatchResult;
import com.company.imticket.infra.cache.SessionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingAppService {

    private final RuleEngineService ruleEngine;
    private final AiClient aiClient;
    private final SessionCacheService sessionCache;

    /**
     * 路由入口：规则优先 → LLM 兜底 → 默认创建工单
     */
    public RouteDecision route(ChannelMessage message, String capitalName, boolean hasOpenTicket) {
        // Step 1: 规则引擎
        RuleMatchResult ruleResult = ruleEngine.match(message.getContent());
        if (ruleResult != null) {
            log.info("route by rule: intent={}, rule={}", ruleResult.getIntent(), ruleResult.getMatchRule());
            return RouteDecision.fromRule(ruleResult);
        }

        // Step 2: 大模型意图识别
        try {
            int kbRounds = sessionCache.incrementKbRound(
                    message.getChannel(), message.getChannelUserId(), message.getChannelGroupId());

            IntentRecognitionContext ctx = IntentRecognitionContext.builder()
                    .capitalName(capitalName)
                    .userName(message.getUserName())
                    .hasOpenTicket(hasOpenTicket)
                    .kbInteractionCount(kbRounds)
                    .message(message.getContent())
                    .build();

            RoutingResult aiResult = aiClient.recognizeIntent(ctx);
            log.info("route by LLM: intent={}, confidence={}, priority={}",
                    aiResult.getIntent(), aiResult.getConfidence(), aiResult.getPriority());
            return RouteDecision.fromLLM(aiResult);

        } catch (Exception e) {
            // Step 3: LLM 异常 → 降级为默认创建工单
            log.error("LLM routing failed, fallback to create_ticket", e);
            return RouteDecision.fallback();
        }
    }
}
```

`RouteDecision.java`:
```java
package com.company.imticket.service.application.routing;

import com.company.imticket.common.enums.RoutingIntent;
import com.company.imticket.infra.ai.dto.RoutingResult;
import com.company.imticket.service.domain.routing.RuleMatchResult;
import lombok.Data;

@Data
public class RouteDecision {
    private RoutingIntent intent;
    private String source;      // RULE / LLM / FALLBACK
    private double confidence;
    private String priority;
    private String sentiment;
    private String extractedParam;  // 提取的工单号等参数

    public static RouteDecision fromRule(RuleMatchResult rule) {
        RouteDecision d = new RouteDecision();
        d.intent = rule.getIntent();
        d.source = "RULE";
        d.confidence = 1.0;
        d.priority = "normal";
        d.extractedParam = rule.getExtractedParam();
        return d;
    }

    public static RouteDecision fromLLM(RoutingResult llm) {
        RouteDecision d = new RouteDecision();
        d.intent = RoutingIntent.valueOf(llm.getIntent().toUpperCase());
        d.source = "LLM";
        d.confidence = llm.getConfidence();
        d.priority = llm.getPriority();
        d.sentiment = llm.getSentiment();
        return d;
    }

    public static RouteDecision fallback() {
        RouteDecision d = new RouteDecision();
        d.intent = RoutingIntent.CREATE_TICKET;
        d.source = "FALLBACK";
        d.confidence = 0.0;
        d.priority = "normal";
        return d;
    }
}
```

### 任务 4.2: 工单应用服务

**目标**: 工单 CRUD + 状态转移 + 分配串联

`im-ticket-service/src/main/java/com/company/imticket/service/application/ticket/TicketAppService.java`:
```java
package com.company.imticket.service.application.ticket;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.TicketException;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.dao.entity.TicketMessage;
import com.company.imticket.dao.mapper.TicketMapper;
import com.company.imticket.dao.mapper.TicketMessageMapper;
import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.channel.ChannelMessage;
import com.company.imticket.infra.notification.NotificationSender;
import com.company.imticket.service.domain.duty.AssignmentResult;
import com.company.imticket.service.domain.duty.DutyAssignmentService;
import com.company.imticket.service.domain.ticket.TicketNoGenerator;
import com.company.imticket.service.domain.ticket.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAppService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper messageMapper;
    private final TicketNoGenerator noGenerator;
    private final TicketStateMachine stateMachine;
    private final DutyAssignmentService dutyAssignment;
    private final NotificationSender notificationSender;
    private final AiClient aiClient;

    /**
     * 创建工单：生成编号 → 分配值班 → 发送通知
     */
    @Transactional
    public Ticket createTicket(ChannelMessage msg, Long capitalId, String capitalName,
                                String priority, String description) {
        // 检查是否有未关闭工单
        Ticket existing = ticketMapper.findOpenTicketByChannelUser(msg.getChannel(), msg.getChannelUserId());
        if (existing != null) {
            // 追加消息到已有工单
            appendMessage(existing, msg);
            throw new TicketException(BizErrorCode.TICKET_HAS_OPEN,
                "已有工单 " + existing.getTicketNo() + " 处理中，消息已追加");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(noGenerator.generate());
        ticket.setCapitalId(capitalId);
        ticket.setChannel(msg.getChannel());
        ticket.setChannelUserId(msg.getChannelUserId());
        ticket.setChannelGroupId(msg.getChannelGroupId());
        ticket.setUserName(msg.getUserName());
        ticket.setDescription(description);
        ticket.setPriority(priority != null ? priority : "normal");
        ticketMapper.insert(ticket);

        // 保存首条消息
        TicketMessage firstMsg = new TicketMessage();
        firstMsg.setTicketId(ticket.getId());
        firstMsg.setSenderType("USER");
        firstMsg.setSenderId(msg.getChannelUserId());
        firstMsg.setSenderName(msg.getUserName());
        firstMsg.setContent(msg.getContent());
        firstMsg.setContentType(msg.getContentType());
        messageMapper.insert(firstMsg);

        // 分配值班客服
        AssignmentResult assign = dutyAssignment.assign(
                msg.getChannel(), msg.getChannelUserId(), msg.getChannelGroupId(), capitalName);
        if (assign != null) {
            ticket.setAssignedAgentId(assign.getAgentId());
            ticketMapper.updateById(ticket);
        }

        // 发送通知
        Map<String, String> vars = Map.of(
            "ticket_id", ticket.getTicketNo(),
            "capital_name", capitalName,
            "user_name", msg.getUserName(),
            "description", description != null ? description : msg.getContent()
        );

        // 通知用户
        notificationSender.sendByTemplate("ticket_created_to_user", vars,
                msg.getChannelUserId(), msg.getChannelGroupId());

        // 通知客服
        if (assign != null) {
            vars.put("agent_name", assign.getAgentName());
            notificationSender.sendByTemplate("ticket_assigned_to_agent", vars,
                    String.valueOf(assign.getAgentId()), null);
        }

        log.info("ticket created: {}, capital={}, user={}, agent={}",
                ticket.getTicketNo(), capitalName, msg.getUserName(),
                assign != null ? assign.getAgentName() : "unassigned");
        return ticket;
    }

    /**
     * 工单状态转移
     */
    @Transactional
    public Ticket transitionStatus(Long ticketId, TicketStatus target,
                                    String operatorId, String operatorType, String remark) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        return stateMachine.transition(ticket, target, operatorId, operatorType, remark);
    }

    /**
     * 分页查询工单列表
     */
    public PageResp<Ticket> listTickets(int page, int size, String channel,
                                         Long capitalId, String status, Long agentId, String keyword) {
        Page<Ticket> p = new Page<>(page, size);
        IPage<Ticket> result = ticketMapper.selectPageWithFilters(p, channel, capitalId, status, agentId, keyword);
        PageResp<Ticket> resp = new PageResp<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }

    /**
     * 查询工单详情（含消息列表）
     */
    public TicketDetail getTicketDetail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        List<TicketMessage> messages = messageMapper.selectByTicketId(ticketId);
        TicketDetail detail = new TicketDetail();
        detail.setTicket(ticket);
        detail.setMessages(messages);
        return detail;
    }

    /**
     * 追加消息到工单
     */
    public void appendMessage(Ticket ticket, ChannelMessage msg) {
        TicketMessage tm = new TicketMessage();
        tm.setTicketId(ticket.getId());
        tm.setSenderType("USER");
        tm.setSenderId(msg.getChannelUserId());
        tm.setSenderName(msg.getUserName());
        tm.setContent(msg.getContent());
        tm.setContentType(msg.getContentType());
        messageMapper.insert(tm);
    }

    /**
     * 客服回复
     */
    public void agentReply(Long ticketId, Long agentId, String agentName, String content) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketException(BizErrorCode.TICKET_NOT_FOUND, "ticketId=" + ticketId);
        }
        TicketMessage msg = new TicketMessage();
        msg.setTicketId(ticketId);
        msg.setSenderType("AGENT");
        msg.setSenderId(String.valueOf(agentId));
        msg.setSenderName(agentName);
        msg.setContent(content);
        msg.setContentType("text");
        messageMapper.insert(msg);
    }
}
```

### 任务 4.3: 知识库应用服务

**目标**: 知识检索 + 大模型生成回答 + 升级判断

`im-ticket-service/src/main/java/com/company/imticket/service/application/knowledge/KnowledgeAppService.java`:
```java
package com.company.imticket.service.application.knowledge;

import com.company.imticket.infra.ai.AiClient;
import com.company.imticket.infra.cache.SessionCacheService;
import com.company.imticket.service.domain.knowledge.KnowledgeSearchResult;
import com.company.imticket.service.domain.knowledge.KnowledgeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAppService {

    private final KnowledgeSearchService searchService;
    private final AiClient aiClient;
    private final SessionCacheService sessionCache;

    @Value("${im.knowledge.escalation-rounds:3}")
    private int escalationRounds;

    /**
     * 知识库问答。返回 null 时需要升级为工单。
     */
    public KnowledgeAnswerResult answer(String channel, String userId, String groupId, String question) {
        // 三层检索
        KnowledgeSearchResult searchResult = searchService.search(question);

        if (searchResult == null) {
            // 完全未命中 → 检查是否需要升级
            int rounds = sessionCache.incrementKbRound(channel, userId, groupId);
            if (rounds >= escalationRounds) {
                log.info("KB escalation triggered: rounds={}, threshold={}", rounds, escalationRounds);
                return KnowledgeAnswerResult.escalate();
            }
            return KnowledgeAnswerResult.noAnswer();
        }

        // 有检索结果 → 用大模型生成最终回答
        String finalAnswer = aiClient.generateKnowledgeAnswer(question, searchResult.getAnswer());
        sessionCache.incrementKbRound(channel, userId, groupId);
        return KnowledgeAnswerResult.answered(finalAnswer, searchResult.getSource());
    }
}
```

`KnowledgeAnswerResult.java`:
```java
package com.company.imticket.service.application.knowledge;

import lombok.Data;

@Data
public class KnowledgeAnswerResult {
    private boolean answered;
    private boolean shouldEscalate;
    private String answer;
    private String source;

    public static KnowledgeAnswerResult answered(String answer, String source) {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.answered = true;
        r.answer = answer;
        r.source = source;
        return r;
    }

    public static KnowledgeAnswerResult noAnswer() {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.answered = false;
        return r;
    }

    public static KnowledgeAnswerResult escalate() {
        KnowledgeAnswerResult r = new KnowledgeAnswerResult();
        r.shouldEscalate = true;
        return r;
    }
}
```

---

## Phase 5: API 层 + WebSocket（2 天）

### 任务 5.1: 全局异常处理

`im-ticket-api/src/main/java/com/company/imticket/api/config/GlobalExceptionHandler.java`:
```java
package com.company.imticket.api.config;

import com.company.imticket.common.dto.R;
import com.company.imticket.common.exception.BizErrorCode;
import com.company.imticket.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("BizException: code={}, detail={}", e.getErrorCode().getCode(), e.getDetail());
        return R.fail(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", detail);
        return R.fail(BizErrorCode.PARAM_INVALID.getCode(), detail);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return R.fail(BizErrorCode.INTERNAL_ERROR);
    }
}
```

### 任务 5.2: 渠道 Webhook Controller

`im-ticket-api/src/main/java/com/company/imticket/api/controller/ChannelWebhookController.java`:
```java
package com.company.imticket.api.controller;

import com.company.imticket.common.dto.R;
import com.company.imticket.infra.channel.*;
import com.company.imticket.service.application.routing.RouteDecision;
import com.company.imticket.service.application.routing.RoutingAppService;
import com.company.imticket.service.application.ticket.TicketAppService;
import com.company.imticket.service.application.knowledge.KnowledgeAnswerResult;
import com.company.imticket.service.application.knowledge.KnowledgeAppService;
import com.company.imticket.common.enums.RoutingIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class ChannelWebhookController {

    private final List<ChannelAdapter> channelAdapters;
    private final RoutingAppService routingAppService;
    private final TicketAppService ticketAppService;
    private final KnowledgeAppService knowledgeAppService;

    /**
     * 企微回调入口
     */
    @PostMapping("/wecom")
    public String wecomCallback(@RequestParam(required = false) String signature,
                                 @RequestParam(required = false) String timestamp,
                                 @RequestParam(required = false) String nonce,
                                 @RequestBody String body) {
        ChannelAdapter adapter = findAdapter("wecom");
        Map<String, Object> payload = parseBody(body);

        // 验签
        if (!adapter.verifySignature(signature, timestamp, nonce, body)) {
            log.error("WeCom signature verification failed");
            return "fail";
        }

        return processMessage(adapter, payload);
    }

    /**
     * 飞书回调入口
     */
    @PostMapping("/feishu")
    public Map<String, Object> feishuCallback(@RequestBody Map<String, Object> body) {
        ChannelAdapter adapter = findAdapter("feishu");
        // 飞书 URL 验证
        if ("url_verification".equals(body.get("type"))) {
            return Map.of("challenge", body.get("challenge"));
        }
        processMessage(adapter, body);
        return Map.of("code", 0);
    }

    private String processMessage(ChannelAdapter adapter, Map<String, Object> payload) {
        ChannelMessage msg = adapter.normalize(payload);
        CapitalIdentity identity = adapter.resolveCapital(payload);

        String capitalName = identity != null ? identity.getCapitalName() : "未知资方";
        Long capitalId = identity != null ? identity.getCapitalId() : null;

        // 智能路由
        boolean hasOpenTicket = hasOpenTicketForUser(msg.getChannel(), msg.getChannelUserId());
        RouteDecision decision = routingAppService.route(msg, capitalName, hasOpenTicket);

        switch (decision.getIntent()) {
            case TICKET_QUERY:
                // 查询工单
                handleTicketQuery(adapter, msg, decision.getExtractedParam());
                break;

            case KNOWLEDGE_QUERY:
                // 知识库问答
                KnowledgeAnswerResult kbResult = knowledgeAppService.answer(
                        msg.getChannel(), msg.getChannelUserId(), msg.getChannelGroupId(), msg.getContent());
                if (kbResult.isShouldEscalate()) {
                    ticketAppService.createTicket(msg, capitalId, capitalName, decision.getPriority(), msg.getContent());
                } else if (kbResult.isAnswered()) {
                    adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text", kbResult.getAnswer());
                } else {
                    adapter.sendMessage(msg.getChannelUserId(), msg.getChannelGroupId(), "text",
                            "暂不支持该查询，如需帮助请直接描述问题，我们将为您创建工单。");
                }
                break;

            case CREATE_TICKET:
            case URGENT_ESCALATION:
            default:
                // 创建工单
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
                .orElseThrow(() -> new IllegalArgumentException("Unsupported channel: " + channel));
    }

    private boolean hasOpenTicketForUser(String channel, String channelUserId) {
        return ticketAppService.ticketMapper.findOpenTicketByChannelUser(channel, channelUserId) != null;
    }

    private void handleTicketQuery(ChannelAdapter adapter, ChannelMessage msg, String ticketNo) {
        // TODO: 调用 TicketAppService 查询并返回
    }

    private Map<String, Object> parseBody(String body) {
        // 使用 Jackson 解析 JSON
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook body", e);
            return Map.of();
        }
    }
}
```

### 任务 5.3: 工单管理 API

`im-ticket-api/src/main/java/com/company/imticket/api/controller/TicketController.java`:
```java
package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.common.enums.TicketStatus;
import com.company.imticket.dao.entity.Ticket;
import com.company.imticket.service.application.ticket.TicketAppService;
import com.company.imticket.service.application.ticket.TicketDetail;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketAppService ticketAppService;

    /**
     * 工单列表（分页 + 筛选）
     */
    @GetMapping
    public R<PageResp<Ticket>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long capitalId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String keyword) {
        return R.ok(ticketAppService.listTickets(page, size, channel, capitalId, status, agentId, keyword));
    }

    /**
     * 工单详情（含消息记录）
     */
    @GetMapping("/{id}")
    public R<TicketDetail> detail(@PathVariable Long id) {
        return R.ok(ticketAppService.getTicketDetail(id));
    }

    /**
     * 客服领取工单
     */
    @PostMapping("/{id}/accept")
    public R<Ticket> accept(@PathVariable Long id, @RequestParam Long agentId) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.IN_PROGRESS,
                String.valueOf(agentId), "AGENT", "客服领取"));
    }

    /**
     * 客服回复
     */
    @PostMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @RequestParam Long agentId,
                          @RequestParam String agentName, @RequestBody String content) {
        ticketAppService.agentReply(id, agentId, agentName, content);
        return R.ok(null);
    }

    /**
     * 转交工单
     */
    @PostMapping("/{id}/transfer")
    public R<Ticket> transfer(@PathVariable Long id, @RequestParam Long agentId,
                               @RequestParam Long targetAgentId, @RequestParam(required = false) String remark) {
        Ticket ticket = ticketAppService.transitionStatus(id, TicketStatus.TRANSFERRED,
                String.valueOf(agentId), "AGENT", "转交给客服" + targetAgentId + ": " + remark);
        return R.ok(ticket);
    }

    /**
     * 驳回工单
     */
    @PostMapping("/{id}/reject")
    public R<Ticket> reject(@PathVariable Long id, @RequestParam Long agentId,
                             @RequestParam String reason) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.REJECTED,
                String.valueOf(agentId), "AGENT", reason));
    }

    /**
     * 延期工单
     */
    @PostMapping("/{id}/defer")
    public R<Ticket> defer(@PathVariable Long id, @RequestParam Long agentId,
                            @RequestParam String reason) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.DEFERRED,
                String.valueOf(agentId), "AGENT", reason));
    }

    /**
     * 标记已解决
     */
    @PostMapping("/{id}/resolve")
    public R<Ticket> resolve(@PathVariable Long id, @RequestParam Long agentId,
                              @RequestParam(required = false) String remark) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.RESOLVED,
                String.valueOf(agentId), "AGENT", remark));
    }

    /**
     * 关闭工单
     */
    @PostMapping("/{id}/close")
    public R<Ticket> close(@PathVariable Long id, @RequestParam String operatorId,
                            @RequestParam String operatorType) {
        return R.ok(ticketAppService.transitionStatus(id, TicketStatus.CLOSED,
                operatorId, operatorType, "关闭工单"));
    }
}
```

### 任务 5.4: 系统管理 API

**文件清单**（均在 `im-ticket-api/src/main/java/com/company/imticket/api/controller/`）:

- `CapitalController.java` — 资方 CRUD: `GET/POST/PUT/DELETE /api/v1/capitals`
- `AgentController.java` — 客服管理 + 在线状态: `GET/POST/PUT/DELETE /api/v1/agents`, `PUT /api/v1/agents/{id}/status`
- `ShiftController.java` — 班次 + 排班: `GET/POST/PUT/DELETE /api/v1/shifts`, `/api/v1/schedules`
- `KnowledgeController.java` — FAQ/文档管理: `GET/POST/PUT/DELETE /api/v1/knowledge/faqs`, `/documents`
- `NotificationTemplateController.java` — 通知模板: `GET/POST/PUT/DELETE /api/v1/notification-templates`

### 任务 5.5: WebSocket 配置

**目标**: 客服工作台实时接收新工单推送

`im-ticket-api/src/main/java/com/company/imticket/api/websocket/WebSocketConfig.java`:
```java
package com.company.imticket.api.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");    // 客户端订阅前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

`im-ticket-api/src/main/java/com/company/imticket/api/websocket/TicketWebSocketHandler.java`:
```java
package com.company.imticket.api.websocket;

import com.company.imticket.dao.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送新工单通知给指定客服
     */
    public void pushNewTicketToAgent(Long agentId, Ticket ticket) {
        messagingTemplate.convertAndSend(
                "/topic/agent/" + agentId + "/new-ticket",
                ticket
        );
    }

    /**
     * 推送工单状态变更
     */
    public void pushTicketStatusChange(Ticket ticket) {
        messagingTemplate.convertAndSend(
                "/topic/ticket/" + ticket.getId() + "/status",
                Map.of("ticketNo", ticket.getTicketNo(), "status", ticket.getStatus())
        );
    }

    /**
     * 推送给所有值班负责人 — 待分配池告警
     */
    public void pushUnassignedAlert(int pendingCount) {
        messagingTemplate.convertAndSend(
                "/topic/duty/unassigned-alert",
                Map.of("pendingCount", pendingCount, "timestamp", System.currentTimeMillis())
        );
    }
}
```

---

## Phase 6: 前端工作台（5 天）

### 任务 6.1: Vue 3 项目初始化

**目标**: 用 Vite 创建 Vue 3 + TypeScript 项目，安装 Element Plus

```bash
cd im-ticket-web
npm create vite@latest . -- --template vue-ts
npm install element-plus pinia vue-router@4 axios sockjs-client stompjs
npm install -D @types/sockjs-client @types/stompjs
```

`im-ticket-web/vite.config.ts`:
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8082', changeOrigin: true },
      '/ws': { target: 'http://localhost:8082', ws: true }
    }
  }
})
```

### 任务 6.2: 路由与布局骨架

**页面路由**: `/tickets`（工单工作台）、`/duty`（值班面板）、`/admin`（系统管理）

`im-ticket-web/src/router/index.ts`:
```typescript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/tickets'
  },
  {
    path: '/tickets',
    name: 'Tickets',
    component: () => import('@/views/tickets/TicketList.vue'),
    meta: { title: '工单工作台', icon: 'Ticket' }
  },
  {
    path: '/tickets/:id',
    name: 'TicketDetail',
    component: () => import('@/views/tickets/TicketDetail.vue'),
    meta: { title: '工单详情' }
  },
  {
    path: '/duty',
    name: 'Duty',
    component: () => import('@/views/duty/DutyPanel.vue'),
    meta: { title: '值班面板', icon: 'Monitor' }
  },
  {
    path: '/admin',
    name: 'Admin',
    redirect: '/admin/capitals',
    children: [
      { path: 'capitals', component: () => import('@/views/admin/CapitalManage.vue'), meta: { title: '资方管理' } },
      { path: 'agents', component: () => import('@/views/admin/AgentManage.vue'), meta: { title: '客服管理' } },
      { path: 'shifts', component: () => import('@/views/admin/ShiftManage.vue'), meta: { title: '值班排班' } },
      { path: 'templates', component: () => import('@/views/admin/TemplateManage.vue'), meta: { title: '通知模板' } },
      { path: 'knowledge', component: () => import('@/views/admin/KnowledgeManage.vue'), meta: { title: '知识库' } }
    ],
    meta: { title: '系统管理', icon: 'Setting' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

`im-ticket-web/src/App.vue` — 三栏布局骨架:
```vue
<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #1a237e">
      <el-menu :default-active="activeMenu" router background-color="#1a237e"
               text-color="#ffffff90" active-text-color="#ffffff" style="border-right: none">
        <div style="padding: 16px; color: white; font-weight: bold; font-size: 14px">
          IM工单系统
        </div>
        <el-menu-item index="/tickets">📋 工单工作台</el-menu-item>
        <el-menu-item index="/duty">📊 值班面板</el-menu-item>
        <el-sub-menu index="/admin">
          <template #title>⚙️ 系统管理</template>
          <el-menu-item index="/admin/capitals">资方管理</el-menu-item>
          <el-menu-item index="/admin/agents">客服管理</el-menu-item>
          <el-menu-item index="/admin/shifts">值班排班</el-menu-item>
          <el-menu-item index="/admin/templates">通知模板</el-menu-item>
          <el-menu-item index="/admin/knowledge">知识库</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>
```

### 任务 6.3: 工单列表页

`im-ticket-web/src/views/tickets/TicketList.vue`:
```vue
<template>
  <div>
    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true" :model="filters">
        <el-form-item label="渠道">
          <el-select v-model="filters.channel" clearable placeholder="全部" style="width: 120px">
            <el-option label="企业微信" value="wecom" />
            <el-option label="飞书" value="feishu" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已转交" value="TRANSFERRED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已延期" value="DEFERRED" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="待确认" value="WAITING_CONFIRM" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="工单编号/资方/用户" clearable style="width: 250px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工单表格 -->
    <el-card>
      <el-table :data="tickets" stripe style="width: 100%" @row-click="openDetail">
        <el-table-column prop="ticketNo" label="工单编号" width="180" />
        <el-table-column prop="capitalId" label="资方" width="120" />
        <el-table-column label="渠道" width="80">
          <template #default="{ row }">
            <el-tag :type="row.channel === 'wecom' ? 'success' : 'primary'" size="small">
              {{ row.channel === 'wecom' ? '企微' : '飞书' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="问题摘要" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="assignedAgentId" label="负责" width="80" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
      <div style="margin-top: 16px; text-align: right">
        <el-pagination v-model:current-page="page" :total="total" :page-size="size"
                       @current-change="loadData" layout="total, prev, pager, next" />
      </div>
    </el-card>
  </div>
</template>
```

对应的 `<script setup lang="ts">` 部分包含 API 调用逻辑（通过 Axios 调用 `/api/v1/tickets`）。

### 任务 6.4: 工单详情页（右侧面板）

`im-ticket-web/src/views/tickets/TicketDetail.vue`:
- 左侧：工单信息表单（编号、资方、渠道、用户、状态、优先级、标签、时间）
- 相似工单推荐区域（调用 `/api/v1/tickets/{id}/similar`）
- 中间：对话记录时间线（TicketMessage 列表）
- 底部：操作栏（回复输入框 + 转交/驳回/延期/解决/关闭按钮）
- WebSocket 订阅 `/topic/ticket/{id}/status` 实时更新

### 任务 6.5: 值班面板 + 管理页面

`/duty` 页面包含：
- 当前值班人员列表（姓名 + 在线状态）
- 待分配工单数（实时更新）
- 超时未响应告警列表
- 手动指派功能

`/admin` 子页面：标准的 Element Plus CRUD 表单页面（表格 + 新增/编辑对话框）

---

## Phase 7: 集成测试与联调（3 天）

### 任务 7.1: 集成测试环境搭建

- Docker Compose 编排 MySQL + Redis + RabbitMQ + Elasticsearch
- 导入初始化 SQL
- 注入测试数据：2 个资方、3 个客服、1 个班次、5 条 FAQ、1 个通知模板

### 任务 7.2: E2E 旅程测试

**旅程 1: 知识库自动应答**
```
POST /api/v1/webhook/wecom  { Content: "审批流程是什么?", FromUserName: "test_user_1" }
→ 预期: KB FAQ 命中 → 返回审批流程答案 → 无工单创建
```

**旅程 2: KB 升级创建工单**
```
连续发送 3 条知识类消息
→ 预期: 第 3 条触发升级 → 工单 IM-xxx 创建 → 分配值班客服 → 通知发送
```

**旅程 3: 工单完整生命周期**
```
创建工单 → 客服领取 → 回复 → 标记已解决 → 用户确认 → 关闭
→ 验证: 8 个状态全部可转移，状态流水记录完整
```

### 任务 7.3: 并发测试

- 模拟 50 个用户在 5 个群聊并发发送消息
- 验证：无重复分配工单、无消息丢失、Redis 分布式锁有效

---

## 附录：开发约定

### Commit 风格

```
feat(ticket): 工单创建与状态转移
fix(routing): LLM路由超时降级失败
test(ticket): 状态机全部转移路径单元测试
docs: 更新API接口文档
```

### 分支策略

```
main          ← 生产就绪
├── develop   ← 开发主线
│   ├── feat/ticket-state-machine
│   ├── feat/channel-adapter
│   └── ...
```

### Code Review 检查项

- [ ] 工单状态转移是否通过状态机（不是直接 setStatus）
- [ ] 外部 API 调用是否有超时和降级
- [ ] 日志包含 traceId（MDC）
- [ ] 新文件遵循包命名规范
- [ ] 单元测试覆盖核心路径
- [ ] SQL 中敏感字段有脱敏处理