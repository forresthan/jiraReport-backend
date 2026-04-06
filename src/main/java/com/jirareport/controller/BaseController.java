package com.jirareport.controller;

import com.jirareport.common.result.Result;
import com.jirareport.common.util.CommonUtil;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class BaseController {

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    @ModelAttribute
    public void setRequestAndResponse(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    protected <T> Result<T> success() {
        return Result.success();
    }

    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    protected <T> Result<T> success(String message, T data) {
        return Result.success(message, data);
    }

    protected <T> Result<T> error() {
        return Result.error();
    }

    protected <T> Result<T> error(String message) {
        return Result.error(message);
    }

    protected <T> Result<T> error(int code, String message) {
        return Result.error(code, message);
    }

    protected Long getCurrentUserId() {
        return CommonUtil.getCurrentUserId();
    }

    protected String getCurrentUsername() {
        return CommonUtil.getCurrentUsername();
    }
}