package com.jirareport.common.web;

import com.jirareport.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

public final class HttpRequestUser {

    private HttpRequestUser() {
    }

    /**
     * JWT 过滤器写入的 JIRA user key（与 project 表 user 字段语义一致时使用 claim userId）。
     */
    public static String requireOwnerId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        String userId = raw != null ? raw.toString() : null;
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(401, "未登录或凭证无效");
        }
        return userId;
    }
}
