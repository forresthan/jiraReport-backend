package com.jirareport.controller.report;

import com.jirareport.common.web.HttpRequestUser;
import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.SummaryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@Tag(name = "总结报告", description = "报告 CRUD、提交与复制")
public class SummaryReportController extends BaseController {

    private final SummaryReportService summaryReportService;

    public SummaryReportController(SummaryReportService summaryReportService) {
        this.summaryReportService = summaryReportService;
    }

    @GetMapping
    @Operation(summary = "报告列表")
    public Result<List<Map<String, Object>>> list(
            HttpServletRequest request,
            @Parameter(description = "JIRA 项目 key") @RequestParam(required = false) String projectKey,
            @Parameter(description = "draft|submitted|reviewed") @RequestParam(required = false) String status) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(summaryReportService.list(ownerId, projectKey, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "报告详情")
    public Result<Map<String, Object>> get(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(summaryReportService.getById(ownerId, id));
    }

    @PostMapping
    @Operation(summary = "新建报告")
    public Result<Map<String, Object>> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String projectKey = str(body.get("projectKey"));
        String projectName = str(body.get("projectName"));
        String title = str(body.get("title"));
        String content = str(body.get("content"));
        Long templateId = toLong(body.get("templateId"));
        return success(summaryReportService.create(ownerId, projectKey, projectName, title, content, templateId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告")
    public Result<Map<String, Object>> update(HttpServletRequest request, @PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String title = body.containsKey("title") ? str(body.get("title")) : null;
        String content = body.containsKey("content") ? str(body.get("content")) : null;
        String status = body.containsKey("status") ? str(body.get("status")) : null;
        String projectKey = body.containsKey("projectKey") ? str(body.get("projectKey")) : null;
        String projectName = body.containsKey("projectName") ? str(body.get("projectName")) : null;
        Long templateId = body.containsKey("templateId") ? toLong(body.get("templateId")) : null;
        String slidesJson = body.containsKey("slidesJson") ? str(body.get("slidesJson")) : null;
        return success(summaryReportService.update(ownerId, id, title, content, status, projectKey, projectName, templateId, slidesJson));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        summaryReportService.delete(ownerId, id);
        return success();
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制报告")
    public Result<Map<String, Object>> copy(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(summaryReportService.copy(ownerId, id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交报告（草稿→已提交）")
    public Result<Map<String, Object>> submit(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(summaryReportService.submit(ownerId, id));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
