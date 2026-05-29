package com.company.imticket.common.exception;

public enum BizErrorCode {
    SUCCESS(0, "success"),
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或Token已过期"),
    FORBIDDEN(40300, "无操作权限"),
    NOT_FOUND(40400, "资源不存在"),
    INTERNAL_ERROR(50000, "系统内部错误"),
    TICKET_NOT_FOUND(10001, "工单不存在"),
    TICKET_STATUS_ILLEGAL(10002, "当前工单状态不允许此操作"),
    TICKET_ALREADY_ASSIGNED(10003, "工单已被其他客服领取"),
    TICKET_HAS_OPEN(10004, "该会话已存在未关闭工单"),
    ROUTING_LLM_TIMEOUT(11001, "大模型路由超时"),
    ROUTING_LLM_ERROR(11002, "大模型路由服务异常"),
    ROUTING_RULE_PARSE_ERROR(11003, "路由规则解析失败"),
    KB_FAQ_DUPLICATE(12001, "FAQ问题已存在"),
    KB_DOC_PARSE_ERROR(12002, "文档解析失败"),
    KB_VECTOR_STORE_ERROR(12003, "向量存储异常"),
    KB_SEARCH_TIMEOUT(12004, "知识库搜索超时"),
    CHANNEL_SIGNATURE_INVALID(13001, "渠道回调签名验证失败"),
    CHANNEL_SEND_FAILED(13002, "渠道消息发送失败"),
    CHANNEL_UNSUPPORTED(13003, "不支持的渠道类型"),
    CHANNEL_USER_NOT_FOUND(13004, "渠道用户未找到"),
    DUTY_NO_AGENT_AVAILABLE(14001, "当前无可用的值班客服"),
    DUTY_SHIFT_CONFLICT(14002, "排班时间冲突"),
    NOTIFICATION_TEMPLATE_NOT_FOUND(15001, "通知模板不存在"),
    NOTIFICATION_SEND_FAILED(15002, "通知发送失败"),
    NOTIFICATION_TEMPLATE_PARSE_ERROR(15003, "通知模板解析失败");

    private final int code;
    private final String message;
    BizErrorCode(int code, String message) { this.code = code; this.message = message; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
}