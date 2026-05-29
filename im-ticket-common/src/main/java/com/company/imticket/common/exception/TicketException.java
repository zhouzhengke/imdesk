package com.company.imticket.common.exception;

public class TicketException extends BizException {
    public TicketException(BizErrorCode errorCode) {
        super(errorCode);
    }

    public TicketException(BizErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}