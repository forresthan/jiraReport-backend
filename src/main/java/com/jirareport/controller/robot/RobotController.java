package com.jirareport.controller.robot;

import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.mapper.entity.RobotConfig;
import com.jirareport.service.RobotService;
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
@RequestMapping("/robot")
@Tag(name = "Robot Reminder", description = "Robot reminder configuration and notification APIs")
public class RobotController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(RobotController.class);

    private final RobotService robotService;

    public RobotController(RobotService robotService) {
        this.robotService = robotService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get robot configurations", description = "Get all robot configurations")
    public Result<List<RobotConfig>> getConfigs() {
        log.info("Get all robot configurations");
        List<RobotConfig> configs = robotService.getConfigs();
        return success(configs);
    }

    @PostMapping("/config")
    @Operation(summary = "Create robot configuration", description = "Create a new robot configuration")
    public Result<RobotConfig> createConfig(@RequestBody RobotConfig config) {
        Long userId = getCurrentUserId();
        log.info("Create robot config - userId: {}", userId);
        RobotConfig created = robotService.saveConfig(config);
        return success(created);
    }

    @PutMapping("/config/{id}")
    @Operation(summary = "Update robot configuration", description = "Update an existing robot configuration")
    public Result<RobotConfig> updateConfig(
            @Parameter(description = "Robot config ID") @PathVariable Long id,
            @RequestBody RobotConfig config) {
        Long userId = getCurrentUserId();
        log.info("Update robot config - userId: {}, id: {}", userId, id);
        config.setId(id);
        RobotConfig updated = robotService.saveConfig(config);
        return success(updated);
    }

    @DeleteMapping("/config/{id}")
    @Operation(summary = "Delete robot configuration", description = "Delete a robot configuration")
    public Result<Boolean> deleteConfig(
            @Parameter(description = "Robot config ID") @PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("Delete robot config - userId: {}, id: {}", userId, id);
        boolean result = robotService.deleteConfig(id);
        return success(result);
    }

    @PostMapping("/remind/bs")
    @Operation(summary = "Manual trigger business requirement reminder", description = "Manually trigger business requirement confirmation reminder")
    public Result<Map<String, Object>> remindBS() {
        Long userId = getCurrentUserId();
        log.info("Manual trigger business requirement reminder - userId: {}", userId);
        Map<String, Object> result = robotService.doRemindBS();
        return success(result);
    }
}