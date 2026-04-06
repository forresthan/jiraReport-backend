package com.jirareport.controller.gitlab;

import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.GitlabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/gitlab")
@Tag(name = "GitLab Integration", description = "GitLab integration APIs")
public class GitlabController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(GitlabController.class);

    private final GitlabService gitlabService;

    public GitlabController(GitlabService gitlabService) {
        this.gitlabService = gitlabService;
    }

    @GetMapping("/projects")
    @Operation(summary = "获取项目列表", description = "获取 GitLab 项目列表")
    public Result<List<Map<String, Object>>> getProjects() {
        log.info("Get GitLab projects");
        List<Map<String, Object>> projects = gitlabService.getProjects();
        return success(projects);
    }

    @GetMapping("/projects/{id}/commits")
    @Operation(summary = "获取提交记录", description = "获取指定项目的提交记录")
    public Result<List<Map<String, Object>>> getProjectCommits(
            @Parameter(description = "项目 ID") @PathVariable String id,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int perPage) {
        log.info("Get commits for project: {}", id);
        List<Map<String, Object>> commits = gitlabService.getProjectCommits(id, perPage);
        return success(commits);
    }

    @GetMapping("/projects/{id}/commits/all")
    @Operation(summary = "分页获取提交记录", description = "分页获取指定项目的所有提交记录")
    public Result<List<Map<String, Object>>> getProjectCommitsLoop(
            @Parameter(description = "项目 ID") @PathVariable String id,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "100") int perPage) {
        log.info("Get all commits for project: {} via pagination", id);
        List<Map<String, Object>> commits = gitlabService.getProjectCommitsLoop(id, perPage);
        return success(commits);
    }

    @PostMapping("/sync")
    @Operation(summary = "手动触发同步", description = "手动触发当前用户的 GitLab 数据同步")
    public Result<String> syncGitlabData() {
        Long userId = getCurrentUserId();
        log.info("Manual sync GitLab data for user: {}", userId);
        String result = gitlabService.syncGitlabData(userId);
        return success(result);
    }

    @PostMapping("/sync/user/{username}")
    @Operation(summary = "按用户同步", description = "按用户名同步 GitLab 数据")
    public Result<String> syncGitlabDataByUser(
            @Parameter(description = "用户名") @PathVariable String username) {
        log.info("Sync GitLab data for user: {}", username);
        String result = gitlabService.syncGitlabDataByUser(username);
        return success(result);
    }

    @PostMapping("/sync/all")
    @Operation(summary = "全量同步", description = "全量同步所有 GitLab 项目数据")
    public Result<String> syncAllGitlabData() {
        log.info("Full sync all GitLab data");
        String result = gitlabService.syncAllGitlabData();
        return success(result);
    }

    @GetMapping("/token/check")
    @Operation(summary = "验证 Token", description = "验证 GitLab Token 是否有效")
    public Result<Map<String, Object>> checkToken() {
        log.info("Check GitLab token");
        boolean valid = gitlabService.checkToken();
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        if (valid) {
            Map<String, Object> userInfo = gitlabService.getCurrentUser();
            result.put("user", userInfo);
        }
        return success(result);
    }

    @GetMapping("/stat")
    @Operation(summary = "获取代码统计", description = "获取当前用户的代码统计")
    public Result<Map<String, Object>> getStat(
            @Parameter(description = "项目名称") @RequestParam(required = false) String project,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Long userId = getCurrentUserId();
        log.info("Get GitLab stat for user: {}, project: {}, {} - {}", userId, project, startDate, endDate);
        Map<String, Object> stat = gitlabService.getStatByUserId(userId, project, startDate, endDate);
        return success(stat);
    }

    @GetMapping("/stat/{username}")
    @Operation(summary = "获取用户代码统计", description = "获取指定用户的代码统计")
    public Result<Map<String, Object>> getStatByUsername(
            @Parameter(description = "用户名") @PathVariable String username,
            @Parameter(description = "项目名称") @RequestParam(required = false) String project,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        log.info("Get GitLab stat for username: {}, project: {}, {} - {}", username, project, startDate, endDate);
        Map<String, Object> stat = gitlabService.getStatByUser(username, project, startDate, endDate);
        return success(stat);
    }
}