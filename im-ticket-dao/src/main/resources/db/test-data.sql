-- ============================================================
-- IM Ticket System - Integration Test Data
-- ============================================================

-- ============================================================
-- 资方测试数据 (2 records)
-- ============================================================
INSERT INTO im_capital (id, name, contact_person, contact_phone, contract_start, contract_end, status, remark)
VALUES
(1, 'XX银行', '王经理', '13800001111', '2025-01-01', '2027-12-31', 1, '主要合作资方，日活用户量大'),
(2, 'YY金融', '赵总监', '13900002222', '2025-06-01', '2026-12-31', 1, '新接入资方，贷款业务为主');

-- ============================================================
-- 资方渠道映射测试数据
-- ============================================================
INSERT INTO im_capital_channel (id, capital_id, channel, identifier_type, identifier_value)
VALUES
(1, 1, 'WECOM', 'corp_id', 'ww_xxbank_corp_001'),
(2, 1, 'FEISHU', 'app_id', 'cli_xxbank_app_001'),
(3, 2, 'WECOM', 'corp_id', 'ww_yyfinance_corp_001'),
(4, 2, 'FEISHU', 'app_id', 'cli_yyfinance_app_001');

-- ============================================================
-- 客服测试数据 (3 records)
-- ============================================================
INSERT INTO im_agent (id, username, password, name, email, phone, role, status, wecom_user_id, feishu_open_id)
VALUES
(1, 'zhangsan',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'zhangsan@company.com', '13800000001', 'AGENT',        'OFFLINE', 'zhangsan_wx', 'ou_zhangsan_fs'),
(2, 'lisi',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'lisi@company.com',     '13800000002', 'ADMIN',        'OFFLINE', 'lisi_wx',     'ou_lisi_fs'),
(3, 'wangwu',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', 'wangwu@company.com',   '13800000003', 'DUTY_MANAGER', 'OFFLINE', 'wangwu_wx',   'ou_wangwu_fs');

-- ============================================================
-- 值班班次测试数据 (1 record)
-- ============================================================
INSERT INTO im_shift (id, name, start_time, end_time, duty_type, primary_agent_ids, backup_agent_ids)
VALUES
(1, '白班', '09:00:00', '18:00:00', 'weekday', '[1]', '[2,3]');

-- ============================================================
-- 排班测试数据 (当日排班)
-- ============================================================
INSERT INTO im_shift_schedule (id, schedule_date, shift_id, agent_ids)
VALUES
(1, CURDATE(), 1, '[1,2]');

-- ============================================================
-- 知识库FAQ测试数据 (5 records)
-- ============================================================
INSERT INTO im_knowledge_faq (id, question, answer, keywords, category, hit_count, enabled)
VALUES
(1, '审批流程是什么？',
   '审批流程分为三步：1）提交申请材料；2）风控审核（1-3个工作日）；3）审批通过后放款。如需加急，请联系客服处理。',
   '审批,流程,审核,申请',
   '贷款流程', 128, 1),

(2, '如何修改银行卡信息？',
   '修改银行卡信息需要：1）登录APP进入"我的-银行卡管理"；2）点击"更换银行卡"；3）输入新卡号并通过短信验证；4）提交审核。审核通过后生效，通常需要1个工作日。',
   '银行卡,修改,更换,卡号',
   '账户管理', 95, 1),

(3, '还款日可以延期吗？',
   '还款日可以在到期前申请延期，最长可延7天。申请方式：1）APP内"还款-申请延期"；2）联系在线客服。注意：延期会产生额外利息，具体以合同约定为准。',
   '还款,延期,宽限,到期',
   '还款相关', 203, 1),

(4, '客服工作时间是什么？',
   '客服工作时间：工作日 9:00-18:00，节假日 10:00-16:00。非工作时间可通过智能客服自助查询常见问题，紧急情况请拨打客服热线 400-XXX-XXXX。',
   '工作时间,营业时间,客服,几点',
   '常见问题', 312, 1),

(5, '如何查看我的贷款进度？',
   '查看贷款进度：1）登录APP，首页"我的贷款"查看实时进度；2）关注企业微信/飞书消息通知；3）如超过承诺时效未更新，请联系客服查询。',
   '贷款进度,审批进度,查询进度',
   '贷款流程', 176, 1);

-- ============================================================
-- 通知模板测试数据 (1 record)
-- ============================================================
INSERT INTO im_notification_template (id, code, name, direction, channel, format, title, content, enabled)
VALUES
(1, 'ticket_created_to_user', '工单创建通知(用户)',
   'OUTBOUND', 'WECOM', 'text',
   '工单已创建',
   '您好 {{userName}}，您的问题已记录为工单 {{ticketNo}}，我们将尽快为您处理。如有补充信息，可直接回复此消息。',
   1);
