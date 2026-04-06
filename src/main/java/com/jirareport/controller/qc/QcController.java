package com.jirareport.controller.qc;

import com.jirareport.common.result.Result;
import com.jirareport.controller.BaseController;
import com.jirareport.service.QcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/qc")
@Tag(name = "QC Integration", description = "QC (Quality Center) integration APIs")
public class QcController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(QcController.class);

    private final QcService qcService;

    public QcController(QcService qcService) {
        this.qcService = qcService;
    }

    @GetMapping("/user")
    @Operation(summary = "获取当前用户信息", description = "获取当前 QC 系统用户信息")
    public Result<Map<String, Object>> getCurrentUser() {
        log.info("Get current QC user");
        Map<String, Object> user = qcService.getCurrentUser();
        return success(user);
    }

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "按账号/姓名搜索用户列表")
    public Result<List<Map<String, Object>>> getUsers(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        log.info("Search QC users with keyword: {}", keyword);
        List<Map<String, Object>> users = qcService.getUser(keyword);
        return success(users);
    }

    @GetMapping("/partners")
    @Operation(summary = "获取部门同事列表", description = "获取当前用户的部门同事列表")
    public Result<List<Map<String, Object>>> getPartners() {
        log.info("Get QC partners");
        List<Map<String, Object>> partners = qcService.getMyPartner();
        return success(partners);
    }
}
