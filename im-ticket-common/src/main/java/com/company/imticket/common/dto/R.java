package com.company.imticket.common.dto;

import com.company.imticket.common.exception.BizErrorCode;

public class R<T> {
    private int code;
    private String message;
    private T data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

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