-- ============================================================
-- IM Ticket System - Database Initialization Script
-- Version: 1.0.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS im_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE im_ticket;

-- ============================================================
-- 1. 资方表
-- ============================================================
DROP TABLE IF EXISTS im_capital;
CREATE TABLE im_capital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '资方名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contract_start DATE COMMENT '合同开始日期',
    contract_end DATE COMMENT '合同结束日期',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=合作中, 0=已终止',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资方表';

-- ============================================================
-- 2. 资方渠道映射表
-- ============================================================
DROP TABLE IF EXISTS im_capital_channel;
CREATE TABLE im_capital_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    capital_id BIGINT NOT NULL COMMENT '资方ID',
    channel VARCHAR(20) NOT NULL COMMENT 'IM渠道: WECOM/FEISHU',
    identifier_type VARCHAR(30) COMMENT '标识类型: group_id/user_id/corp_id',
    identifier_value VARCHAR(200) COMMENT '标识值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    INDEX idx_capital (capital_id),
    UNIQUE INDEX idx_channel_identifier (channel, identifier_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资方渠道映射表';

-- ============================================================
-- 3. 客服表
-- ============================================================
DROP TABLE IF EXISTS im_agent;
CREATE TABLE im_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(加密存储)',
    name VARCHAR(50) COMMENT '姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    role VARCHAR(20) DEFAULT 'AGENT' COMMENT '角色: ADMIN/AGENT',
    status VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '在线状态: ONLINE/OFFLINE/BUSY',
    wecom_user_id VARCHAR(100) COMMENT '企业微信用户ID',
    feishu_open_id VARCHAR(100) COMMENT '飞书用户OpenID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    UNIQUE INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服表';

-- ============================================================
-- 4. 工单表
-- ============================================================
DROP TABLE IF EXISTS im_ticket;
CREATE TABLE im_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_no VARCHAR(30) NOT NULL COMMENT '工单编号',
    capital_id BIGINT COMMENT '资方ID',
    channel VARCHAR(20) COMMENT '来源渠道: WECOM/FEISHU',
    channel_user_id VARCHAR(200) COMMENT '渠道用户ID',
    channel_group_id VARCHAR(200) COMMENT '渠道群ID',
    user_name VARCHAR(100) COMMENT '用户名称',
    description TEXT COMMENT '问题描述',
    status VARCHAR(30) DEFAULT 'PENDING' COMMENT '工单状态: PENDING/IN_PROGRESS/RESOLVED/CLOSED',
    priority VARCHAR(10) DEFAULT 'normal' COMMENT '优先级: high/normal/low',
    category VARCHAR(50) COMMENT '问题分类',
    tags VARCHAR(500) COMMENT '标签(JSON数组)',
    assigned_agent_id BIGINT COMMENT '指派的客服ID',
    context_summary TEXT COMMENT '上下文摘要(用户历史)',
    resolved_at DATETIME COMMENT '解决时间',
    closed_at DATETIME COMMENT '关闭时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    UNIQUE INDEX idx_ticket_no (ticket_no),
    INDEX idx_capital (capital_id),
    INDEX idx_status_agent (status, assigned_agent_id),
    INDEX idx_channel_created (channel, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- ============================================================
-- 5. 工单消息表
-- ============================================================
DROP TABLE IF EXISTS im_ticket_message;
CREATE TABLE im_ticket_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    sender_type VARCHAR(10) COMMENT '发送者类型: USER/AGENT/SYSTEM',
    sender_id VARCHAR(100) COMMENT '发送者ID',
    sender_name VARCHAR(100) COMMENT '发送者名称',
    content TEXT COMMENT '消息内容',
    content_type VARCHAR(20) DEFAULT 'text' COMMENT '内容类型: text/image/file',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ticket (ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单消息表';

-- ============================================================
-- 6. 工单状态流水表
-- ============================================================
DROP TABLE IF EXISTS im_ticket_state_log;
CREATE TABLE im_ticket_state_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    from_status VARCHAR(30) COMMENT '变更前状态',
    to_status VARCHAR(30) COMMENT '变更后状态',
    operator_id VARCHAR(100) COMMENT '操作人ID',
    operator_type VARCHAR(10) COMMENT '操作人类型: USER/AGENT/SYSTEM',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ticket (ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单状态流水表';

-- ============================================================
-- 7. 值班班次表
-- ============================================================
DROP TABLE IF EXISTS im_shift;
CREATE TABLE im_shift (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '班次名称',
    start_time TIME COMMENT '开始时间',
    end_time TIME COMMENT '结束时间',
    duty_type VARCHAR(20) DEFAULT 'weekday' COMMENT '班次类型: weekday/weekend/holiday',
    primary_agent_ids VARCHAR(500) COMMENT '主值班客服ID列表(JSON数组)',
    backup_agent_ids VARCHAR(500) COMMENT '备班客服ID列表(JSON数组)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='值班班次表';

-- ============================================================
-- 8. 排班表
-- ============================================================
DROP TABLE IF EXISTS im_shift_schedule;
CREATE TABLE im_shift_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    shift_id BIGINT NOT NULL COMMENT '班次ID',
    agent_ids VARCHAR(500) COMMENT '当日值班客服ID列表(JSON数组)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE INDEX idx_date_shift (schedule_date, shift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班表';

-- ============================================================
-- 9. 通知模板表
-- ============================================================
DROP TABLE IF EXISTS im_notification_template;
CREATE TABLE im_notification_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(50) NOT NULL COMMENT '模板编码',
    name VARCHAR(100) COMMENT '模板名称',
    direction VARCHAR(10) COMMENT '消息方向: INBOUND/OUTBOUND',
    channel VARCHAR(20) COMMENT '通知渠道: WECOM/FEISHU/SMS/EMAIL',
    format VARCHAR(10) DEFAULT 'text' COMMENT '消息格式: text/markdown/card',
    title VARCHAR(200) COMMENT '消息标题',
    content TEXT COMMENT '模板内容(支持变量占位符)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    UNIQUE INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- ============================================================
-- 10. 知识库FAQ表
-- ============================================================
DROP TABLE IF EXISTS im_knowledge_faq;
CREATE TABLE im_knowledge_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    question VARCHAR(500) COMMENT '标准问题',
    answer TEXT COMMENT '标准回答',
    keywords VARCHAR(300) COMMENT '关键词(逗号分隔)',
    category VARCHAR(50) COMMENT '分类',
    hit_count BIGINT DEFAULT 0 COMMENT '命中次数',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    FULLTEXT INDEX ft_question_keywords (question, keywords)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库FAQ表';

-- ============================================================
-- 11. 知识库文档表
-- ============================================================
DROP TABLE IF EXISTS im_knowledge_document;
CREATE TABLE im_knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) COMMENT '文档标题',
    file_name VARCHAR(200) COMMENT '文件名',
    file_type VARCHAR(20) COMMENT '文件类型: pdf/docx/txt/md',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    chunk_count INT COMMENT '分块数量',
    es_index_name VARCHAR(100) COMMENT 'ES索引名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- ============================================================
-- 12. 历史工单归档表
-- ============================================================
DROP TABLE IF EXISTS im_knowledge_ticket_archive;
CREATE TABLE im_knowledge_ticket_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT COMMENT '原始工单ID',
    ticket_no VARCHAR(30) COMMENT '工单编号',
    capital_name VARCHAR(100) COMMENT '资方名称',
    summary TEXT COMMENT '问题摘要',
    solution TEXT COMMENT '解决方案',
    keywords VARCHAR(300) COMMENT '关键词(逗号分隔)',
    category VARCHAR(50) COMMENT '问题分类',
    es_doc_id VARCHAR(100) COMMENT 'ES文档ID',
    reviewed_by BIGINT COMMENT '审核人ID',
    reviewed_at DATETIME COMMENT '审核时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ticket_no (ticket_no),
    INDEX idx_keywords (keywords),
    FULLTEXT INDEX ft_summary_solution (summary, solution)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史工单归档表';

-- ============================================================
-- 13. 消息失败队列表
-- ============================================================
DROP TABLE IF EXISTS im_message_fail_queue;
CREATE TABLE im_message_fail_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT COMMENT '关联工单ID',
    channel VARCHAR(20) COMMENT '目标渠道: WECOM/FEISHU',
    channel_user_id VARCHAR(200) COMMENT '渠道用户ID',
    channel_group_id VARCHAR(200) COMMENT '渠道群ID',
    content TEXT COMMENT '消息内容',
    content_type VARCHAR(20) DEFAULT 'text' COMMENT '内容类型: text/markdown/card',
    retry_count INT DEFAULT 0 COMMENT '已重试次数',
    max_retry INT DEFAULT 5 COMMENT '最大重试次数',
    next_retry_at DATETIME COMMENT '下次重试时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/RETRYING/FAILED/SUCCESS',
    error_msg VARCHAR(500) COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status_next (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息失败队列表';