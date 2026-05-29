package com.company.imticket.common.exception;

public class DutyException extends BizException {
    public DutyException(BizErrorCode errorCode) {
        super(errorCode);
    }

    public DutyException(BizErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}