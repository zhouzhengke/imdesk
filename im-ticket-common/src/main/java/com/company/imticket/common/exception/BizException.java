package com.company.imticket.common.exception;

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

    public BizErrorCode getErrorCode() { return errorCode; }
    public String getDetail() { return detail; }
}