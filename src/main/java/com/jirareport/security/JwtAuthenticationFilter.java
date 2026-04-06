package com.jirareport.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String[] WHITE_LIST = {
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        log.info("JWT Filter - Request URI: {}, WhiteList: {}", requestUri, java.util.Arrays.toString(WHITE_LIST));

        if (isWhiteListed(requestUri)) {
            log.info("JWT Filter - Path is whitelisted, allowing: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.info("JWT Filter - No valid auth header, continuing: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (jwtTokenUtil.validateToken(token)) {
                Claims claims = jwtTokenUtil.parseToken(token);
                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();

                UserPrincipal principal = new UserPrincipal(userId, username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                log.info("JWT Filter - Authentication set for user: {}", username);
            }
        } catch (Exception e) {
            logger.error("JWT validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String requestUri) {
        for (String pattern : WHITE_LIST) {
            if (PATH_MATCHER.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    public static class UserPrincipal {
        private final String userId;
        private final String username;

        public UserPrincipal(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
