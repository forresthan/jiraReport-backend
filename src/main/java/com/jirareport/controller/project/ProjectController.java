package com.jirareport.controller.project;

import com.jirareport.controller.BaseController;
import com.jirareport.common.result.Result;
import com.jirareport.mapper.entity.Project;
import com.jirareport.service.ProjectService;
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
@RequestMapping("/project")
@Tag(name = "Project Configuration", description = "Project configuration APIs")
public class ProjectController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all JIRA projects", description = "Get all projects from JIRA")
    public Result<List<Map<String, Object>>> getAllProjects() {
        String userId = getCurrentUsername();
        log.info("Get all projects - userId: {}", userId);
        List<Map<String, Object>> projects = projectService.getAllProjects(userId);
        return success(projects);
    }

    @GetMapping("/index")
    @Operation(summary = "Get user followed projects", description = "Get current user's followed projects list")
    public Result<List<Map<String, Object>>> getUserProjects() {
        String userId = getCurrentUsername();
        log.info("Get user projects - userId: {}", userId);
        List<Map<String, Object>> projects = projectService.getUserProjectsWithDetails(userId);
        return success(projects);
    }

    @PostMapping("/create")
    @Operation(summary = "Add followed project", description = "Add a project to user's followed list")
    public Result<Project> addProject(
            @Parameter(description = "Project key") @RequestParam String projectKey,
            @Parameter(description = "Project name") @RequestParam(required = false) String projectName) {
        String userId = getCurrentUsername();
        log.info("Add project - userId: {}, projectKey: {}", userId, projectKey);
        Project project = projectService.addProject(userId, projectKey, projectName);
        return success(project);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Remove followed project", description = "Remove a project from user's followed list")
    public Result<Boolean> removeProject(
            @Parameter(description = "User project ID") @PathVariable Long projectId) {
        String userId = getCurrentUsername();
        log.info("Remove project - userId: {}, projectId: {}", userId, projectId);
        boolean result = projectService.removeProject(userId, projectId);
        return success(result);
    }

    @PutMapping("/{projectId}/view-mode")
    @Operation(summary = "Update project view mode", description = "Update project view mode (iteration/product/requirement)")
    public Result<Boolean> updateViewMode(
            @Parameter(description = "User project ID") @PathVariable Long projectId,
            @Parameter(description = "View mode") @RequestParam String viewMode) {
        String userId = getCurrentUsername();
        log.info("Update view mode - userId: {}, projectId: {}, viewMode: {}", userId, projectId, viewMode);
        boolean result = projectService.updateViewMode(userId, projectId, viewMode);
        return success(result);
    }
}
