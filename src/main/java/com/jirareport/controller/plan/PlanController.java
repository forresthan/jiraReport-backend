package com.jirareport.controller.plan;

import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.PlanService;
import com.jirareport.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/plan")
@Tag(name = "Project Progress Management", description = "Project progress management APIs")
public class PlanController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanService planService;
    private final IUserService userService;

    public PlanController(PlanService planService, IUserService userService) {
        this.planService = planService;
        this.userService = userService;
    }

    @GetMapping("/issues")
    @Operation(summary = "Get project issue list", description = "Get project issue list with view mode support (iteration/product/requirement)")
    public Result<List<Map<String, Object>>> getIssues(
            HttpServletRequest request,
            @Parameter(description = "Project key") @RequestParam(required = false) String projectKey,
            @Parameter(description = "View mode: iteration, product, or requirement") @RequestParam(required = false, defaultValue = "iteration") String viewMode,
            @Parameter(description = "Sprint or version ID") @RequestParam(required = false) String sprintId) {
        String userId = (String) request.getAttribute("userId");
        log.info("Get issues - projectKey: {}, viewMode: {}, sprintId: {}, userId: {}", projectKey, viewMode, sprintId, userId);
        List<Map<String, Object>> issues = planService.getIssues(userId, projectKey, viewMode, sprintId);
        return success(issues);
    }

    @GetMapping("/versions")
    @Operation(summary = "Get project version list", description = "Get project version list")
    public Result<List<Map<String, Object>>> getVersions(
            HttpServletRequest request,
            @Parameter(description = "Project key") @RequestParam(required = false) String projectKey) {
        String userId = (String) request.getAttribute("userId");
        log.info("Get versions - projectKey: {}, userId: {}", projectKey, userId);
        List<Map<String, Object>> versions = planService.getVersions(userId, projectKey);
        return success(versions);
    }

    @PutMapping("/issues/{key}/priority")
    @Operation(summary = "Update issue priority", description = "Update issue priority")
    public Result<Boolean> updatePriority(
            @Parameter(description = "Issue key") @PathVariable String key,
            @Parameter(description = "Priority") @RequestParam String priority) {
        log.info("Update priority - key: {}, priority: {}", key, priority);
        boolean result = planService.updatePriority(key, priority);
        return success(result);
    }

    @PutMapping("/issues/{key}/labels")
    @Operation(summary = "Update issue labels", description = "Update issue labels")
    public Result<Boolean> updateLabels(
            @Parameter(description = "Issue key") @PathVariable String key,
            @Parameter(description = "Labels") @RequestBody List<String> labels) {
        log.info("Update labels - key: {}, labels: {}", key, labels);
        boolean result = planService.updateLabels(key, labels);
        return success(result);
    }

    @PutMapping("/issues/{key}/order")
    @Operation(summary = "Update issue order", description = "Update issue order")
    public Result<Boolean> updateOrder(
            @Parameter(description = "Issue key") @PathVariable String key,
            @Parameter(description = "Order value") @RequestParam int order) {
        log.info("Update order - key: {}, order: {}", key, order);
        boolean result = planService.updateOrder(key, order);
        return success(result);
    }

    @PutMapping("/issues/{key}/fields")
    @Operation(summary = "Update issue custom fields", description = "Update issue custom fields")
    public Result<Boolean> updateFields(
            @Parameter(description = "Issue key") @PathVariable String key,
            @RequestBody Map<String, Object> fields) {
        log.info("Update fields - key: {}", key);
        boolean result = planService.updateFields(key, fields);
        return success(result);
    }

    @PutMapping("/issues/{key}/fixVersions")
    @Operation(summary = "Update issue fix versions", description = "Update issue fix versions")
    public Result<Boolean> updateFixVersions(
            @Parameter(description = "Issue key") @PathVariable String key,
            @Parameter(description = "Fix versions") @RequestBody List<String> versions) {
        log.info("Update fix versions - key: {}, versions: {}", key, versions);
        boolean result = planService.updateFixVersions(key, versions);
        return success(result);
    }

    @GetMapping("/issues/{key}/subtasks")
    @Operation(summary = "Get issue subtasks", description = "Get issue subtasks")
    public Result<List<Map<String, Object>>> getSubtasks(
            @Parameter(description = "Issue key") @PathVariable String key) {
        log.info("Get subtasks - key: {}", key);
        List<Map<String, Object>> subtasks = planService.getSubtasks(key);
        return success(subtasks);
    }

    @PutMapping("/issues/{key}/subtasks")
    @Operation(summary = "Save subtasks", description = "Save subtasks for an issue")
    public Result<Boolean> saveSubtasks(
            @Parameter(description = "Issue key") @PathVariable String key,
            @RequestBody List<Map<String, Object>> subtasks) {
        log.info("Save subtasks - key: {}", key);
        boolean result = planService.saveSubtasks(key, subtasks);
        return success(result);
    }

    @GetMapping("/issues/{key}/comment")
    @Operation(summary = "Get issue comments", description = "Get issue comments")
    public Result<List<Map<String, Object>>> getComment(
            @Parameter(description = "Issue key") @PathVariable String key) {
        log.info("Get comment - key: {}", key);
        List<Map<String, Object>> comments = planService.getComment(key);
        return success(comments);
    }

    @PutMapping("/issues/{key}/comment")
    @Operation(summary = "Update issue comment", description = "Update issue comment")
    public Result<Boolean> updateComment(
            @Parameter(description = "Issue key") @PathVariable String key,
            @Parameter(description = "Comment ID") @RequestParam String commentId,
            @Parameter(description = "Comment content") @RequestParam String content) {
        log.info("Update comment - key: {}, commentId: {}", key, commentId);
        boolean result = planService.updateComment(key, commentId, content);
        return success(result);
    }

    @GetMapping("/position-stat")
    @Operation(summary = "Get position statistics", description = "Get position statistics by assignee")
    public Result<Map<String, Object>> computedPositionStat(
            HttpServletRequest request,
            @Parameter(description = "Project key") @RequestParam(required = false) String projectKey,
            @Parameter(description = "View mode") @RequestParam(required = false, defaultValue = "iteration") String viewMode) {
        String userId = (String) request.getAttribute("userId");
        log.info("Computed position stat - projectKey: {}, viewMode: {}, userId: {}", projectKey, viewMode, userId);
        Map<String, Object> stat = planService.computedPositionStat(userId, projectKey, viewMode);
        return success(stat);
    }

    @GetMapping("/partner-issues")
    @Operation(summary = "Get partner issues", description = "Get team member issues summary")
    public Result<List<Map<String, Object>>> getPartnerIssues(
            HttpServletRequest request,
            @Parameter(description = "Project key") @RequestParam(required = false) String projectKey,
            @Parameter(description = "View mode") @RequestParam(required = false, defaultValue = "iteration") String viewMode,
            @Parameter(description = "Partner IDs") @RequestParam List<String> partnerIds) {
        String userId = (String) request.getAttribute("userId");
        log.info("Get partner issues - projectKey: {}, viewMode: {}, partnerIds: {}, userId: {}", projectKey, viewMode, partnerIds, userId);
        List<Map<String, Object>> issues = planService.getPartnerIssues(userId, projectKey, viewMode, partnerIds);
        return success(issues);
    }
}