package com.jirareport.controller.user;

import com.jirareport.common.result.Result;
import com.jirareport.common.util.JwtUtil;
import com.jirareport.service.IUserService;
import com.jirareport.service.JiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "用户登录、登出相关接口")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final IUserService userService;
    private final JiraService jiraService;
    private final JwtUtil jwtUtil;

    public UserController(IUserService userService, JiraService jiraService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jiraService = jiraService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用JIRA用户名和密码登录系统")
    public ResponseEntity<Result<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        log.info("用户登录尝试: {}", username);
        
        try {
            boolean jiraValid = jiraService.login(username, password);
            if (!jiraValid) {
                log.warn("JIRA账号验证失败: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "JIRA账号或密码错误"));
            }
            
            Map<String, Object> result = userService.login(username, password);
            log.info("用户登录成功: {}", username);
            
            return ResponseEntity.ok(Result.success(result));
            
        } catch (Exception e) {
            log.error("用户登录异常: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "登录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(@Parameter(description = "用户名") @RequestParam String username) {
        log.info("用户登出: {}", username);
        userService.logout(username);
        return Result.success(null);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public Result<Map<String, Object>> getCurrentUser(
            @Parameter(description = "用户名") @RequestParam String username) {
        Map<String, Object> userInfo = userService.getUserInfo(username);
        return Result.success(userInfo);
    }

    @PostMapping("/check-session")
    @Operation(summary = "检查JIRA Session是否有效", description = "使用浏览器传递的JIRA Cookie验证Session")
    public ResponseEntity<Result<Map<String, Object>>> checkSession(
            @RequestHeader(value = "X-JIRA-Session", required = false) String jiraSession) {
        log.info("检查JIRA Session有效性");

        if (jiraSession == null || jiraSession.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "No JIRA Session"));
        }

        try {
            Map<String, Object> userInfo = jiraService.getCurrentUserBySession(jiraSession);
            log.info("JIRA Session验证成功: {}", userInfo.get("name"));

            String userId = userInfo.get("key").toString();
            userService.storeJiraSession(userId, jiraSession);

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            String username = userInfo.get("name") != null ? userInfo.get("name").toString() : userId;
            String token = jwtUtil.generateToken(username, claims);

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userInfo", userInfo);

            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.warn("JIRA Session验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "JIRA Session无效或已过期"));
        }
    }
}