package com.jirareport.controller.report;

import com.jirareport.common.web.HttpRequestUser;
import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.service.PptHtmlTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ppt-templates")
@Tag(name = "PPT HTML 模板", description = "上传 PPT 转换的 HTML 模板 CRUD")
public class PptHtmlTemplateController extends BaseController {

    private final PptHtmlTemplateService service;

    public PptHtmlTemplateController(PptHtmlTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "模板列表")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(service.list(ownerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情")
    public Result<Map<String, Object>> get(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        return success(service.getById(ownerId, id));
    }

    @PostMapping
    @Operation(summary = "新建模板")
    public Result<Map<String, Object>> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String name = str(body.get("name"));
        String description = str(body.get("description"));
        String htmlContent = str(body.get("htmlContent"));
        String metaJson = str(body.get("metaJson"));
        String originalFileName = str(body.get("originalFileName"));
        return success(service.create(ownerId, name, description, htmlContent, metaJson, originalFileName));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public Result<Map<String, Object>> update(HttpServletRequest request, @PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        String name = body.containsKey("name") ? str(body.get("name")) : null;
        String description = body.containsKey("description") ? str(body.get("description")) : null;
        String htmlContent = body.containsKey("htmlContent") ? str(body.get("htmlContent")) : null;
        String metaJson = body.containsKey("metaJson") ? str(body.get("metaJson")) : null;
        String status = body.containsKey("status") ? str(body.get("status")) : null;
        return success(service.update(ownerId, id, name, description, htmlContent, metaJson, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        String ownerId = HttpRequestUser.requireOwnerId(request);
        service.delete(ownerId, id);
        return success();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
