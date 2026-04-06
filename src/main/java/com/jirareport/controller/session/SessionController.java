package com.jirareport.controller.session;

import com.jirareport.common.result.Result;
import com.jirareport.service.IUserService;
import com.jirareport.service.JiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/session")
@Tag(name = "Session管理", description = "JIRA Session校验")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final IUserService userService;
    private final JiraService jiraService;

    public SessionController(IUserService userService, JiraService jiraService) {
        this.userService = userService;
        this.jiraService = jiraService;
    }

    @GetMapping("/validate")
    @Operation(summary = "校验当前JIRA Session是否有效", description = "使用JWT token获取用户ID，验证Redis中存储的JIRA Session")
    public ResponseEntity<Result<Map<String, Object>>> validateSession(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        log.info("校验JIRA Session有效性, userId: {}", userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "用户未登录"));
        }

        String sessionCookie = userService.getJiraSession(userId);
        if (sessionCookie == null || sessionCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "JIRA Session不存在，请重新登录"));
        }

        try {
            Map<String, Object> userInfo = jiraService.getCurrentUserBySession(sessionCookie);
            log.info("JIRA Session校验成功, userId: {}, name: {}", userId, userInfo.get("name"));

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("userInfo", userInfo);

            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.warn("JIRA Session校验失败, userId: {}, error: {}", userId, e.getMessage());
            userService.removeJiraSession(userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "JIRA Session已过期，请重新登录"));
        }
    }
}
