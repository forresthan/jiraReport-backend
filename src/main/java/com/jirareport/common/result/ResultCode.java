package com.jirareport.common.result;

public enum ResultCode {

    SUCCESS(200, "Success"),
    ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}