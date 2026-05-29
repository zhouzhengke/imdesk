package com.company.imticket.common.exception;

public class ChannelException extends BizException {
    public ChannelException(BizErrorCode errorCode) {
        super(errorCode);
    }

    public ChannelException(BizErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}