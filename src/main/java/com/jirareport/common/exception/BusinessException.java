package com.jirareport.common.exception;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}