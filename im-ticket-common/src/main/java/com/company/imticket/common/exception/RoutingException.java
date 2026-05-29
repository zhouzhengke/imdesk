package com.company.imticket.common.exception;

public class RoutingException extends BizException {
    public RoutingException(BizErrorCode errorCode) {
        super(errorCode);
    }

    public RoutingException(BizErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}