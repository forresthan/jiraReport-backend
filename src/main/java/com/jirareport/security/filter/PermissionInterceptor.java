package com.jirareport.security.filter;

import com.jirareport.common.exception.BusinessException;
import com.jirareport.mapper.entity.User;
import com.jirareport.security.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String ROLE_ADMIN = "role_admin";
    private static final String ROLE_USER = "role_user";

    private static final List<String> ADMIN_PATTERNS = Arrays.asList(
            "/user/delete",
            "/user/admin/**",
            "/system/**"
    );

    private static final List<String> USER_PATTERNS = Arrays.asList(
            "/user/**",
            "/task/**"
    );

    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        User user = userDetailsService.loadUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "用户已被禁用");
        }

        Integer role = user.getRole();
        if (role == null) {
            throw new BusinessException(403, "用户角色未知");
        }

        if (isAdminRequest(requestUri)) {
            if (!ROLE_ADMIN.equals(getRoleName(role))) {
                throw new BusinessException(403, "无管理员权限");
            }
        } else if (isUserRequest(requestUri)) {
            if (!ROLE_ADMIN.equals(getRoleName(role)) && !ROLE_USER.equals(getRoleName(role))) {
                throw new BusinessException(403, "无访问权限");
            }
        }

        request.setAttribute("currentUser", user);
        return true;
    }

    private boolean isAdminRequest(String requestUri) {
        return ADMIN_PATTERNS.stream().anyMatch(pattern -> matchPattern(requestUri, pattern));
    }

    private boolean isUserRequest(String requestUri) {
        return USER_PATTERNS.stream().anyMatch(pattern -> matchPattern(requestUri, pattern));
    }

    private boolean matchPattern(String requestUri, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return requestUri.startsWith(prefix);
        }
        return requestUri.equals(pattern);
    }

    private String getRoleName(Integer role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case 0 -> ROLE_ADMIN;
            case 1 -> ROLE_USER;
            default -> null;
        };
    }
}