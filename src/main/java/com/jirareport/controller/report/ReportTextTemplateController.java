package com.jirareport.controller.report;

import com.jirareport.common.web.HttpRequestUser;
import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.ReportTextTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report-templates")
@Tag(name = "报告文字模板", description = "章节模板 CRUD")
public class ReportTextTemplateController extends BaseController {

    private final ReportTextTemplateService templateService;

    public ReportTextTemplateController(ReportTextTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "模板列表")
    public Result<List<Map<String, Object>>> list(
            HttpServletRequest request,
            @Parameter(description = "按项目 key 过滤") @RequestParam(required = false) String projectKey) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(templateService.list(ownerId, projectKey));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情")
    public Result<Map<String, Object>> get(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(templateService.getById(ownerId, id));
    }

    @PostMapping
    @Operation(summary = "新建模板")
    public Result<Map<String, Object>> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String projectKey = str(body.get("projectKey"));
        String name = str(body.get("name"));
        boolean enabled = body.get("enabled") == null || Boolean.parseBoolean(String.valueOf(body.get("enabled")));
        String sectionsJson = str(body.get("sectionsJson"));
        return success(templateService.create(ownerId, projectKey, name, enabled, sectionsJson));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public Result<Map<String, Object>> update(HttpServletRequest request, @PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String projectKey = body.containsKey("projectKey") ? str(body.get("projectKey")) : null;
        String name = body.containsKey("name") ? str(body.get("name")) : null;
        Boolean enabled = body.containsKey("enabled") ? Boolean.parseBoolean(String.valueOf(body.get("enabled"))) : null;
        String sectionsJson = body.containsKey("sectionsJson") ? str(body.get("sectionsJson")) : null;
        return success(templateService.update(ownerId, id, projectKey, name, enabled, sectionsJson));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        templateService.delete(ownerId, id);
        return success();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
