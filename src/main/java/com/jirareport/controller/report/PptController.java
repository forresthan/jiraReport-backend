package com.jirareport.controller.report;

import com.jirareport.common.web.HttpRequestUser;
import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.PptExportService;
import com.jirareport.service.PptThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ppt")
@Tag(name = "PPT", description = "主题列表与导出任务")
public class PptController extends BaseController {

    private final PptThemeService pptThemeService;
    private final PptExportService pptExportService;

    public PptController(PptThemeService pptThemeService, PptExportService pptExportService) {
        this.pptThemeService = pptThemeService;
        this.pptExportService = pptExportService;
    }

    @GetMapping("/themes")
    @Operation(summary = "PPT 皮肤模板列表（无需登录也可浏览，便于前端展示）")
    public Result<List<Map<String, Object>>> themes() {
        return success(pptThemeService.listThemes());
    }

    @PostMapping("/reports/{reportId}/jobs")
    @Operation(summary = "发起 PPT 导出任务")
    public Result<Map<String, Object>> startJob(HttpServletRequest request,
                                                  @PathVariable Long reportId,
                                                  @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String themeKey = body.get("themeKey") != null ? body.get("themeKey").toString() : null;
        if (themeKey == null || themeKey.isBlank()) {
            throw new IllegalArgumentException("themeKey 必填");
        }
        return success(pptExportService.startJob(ownerId, reportId, themeKey));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "查询导出任务")
    public Result<Map<String, Object>> getJob(HttpServletRequest request, @PathVariable Long jobId) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(pptExportService.getJob(ownerId, jobId));
    }

    @GetMapping("/reports/{reportId}/jobs")
    @Operation(summary = "某报告下的导出任务列表")
    public Result<List<Map<String, Object>>> listJobs(HttpServletRequest request, @PathVariable Long reportId) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(pptExportService.listJobsForReport(ownerId, reportId));
    }
}
