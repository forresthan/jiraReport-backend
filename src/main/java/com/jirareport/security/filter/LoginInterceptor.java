package com.jirareport.security.filter;

import cn.hutool.core.util.StrUtil;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_INFO_KEY = "user:token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(401, "请先登录");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        String redisKey = USER_INFO_KEY + token;
        String cachedToken = redisTemplate.opsForValue().get(redisKey);
        if (cachedToken == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }

        Claims claims = jwtUtil.parseToken(token);
        String username = claims.getSubject();
        String userId = claims.get("userId", String.class);

        request.setAttribute("username", username);
        request.setAttribute("userId", userId);

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (StrUtil.isNotBlank(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}