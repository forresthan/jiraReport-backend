package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.mapper.ProjectMapper;
import com.jirareport.mapper.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectMapper projectMapper;
    private final JiraService jiraService;
    private final IUserService userService;

    public ProjectService(ProjectMapper projectMapper, JiraService jiraService, IUserService userService) {
        this.projectMapper = projectMapper;
        this.jiraService = jiraService;
        this.userService = userService;
    }

    public List<Map<String, Object>> getAllProjects(String userId) {
        String session = userService.getJiraSession(userId);
        return jiraService.getProjectsBySession(session);
    }

    public List<Project> getUserProjects(String userId) {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId);
        wrapper.orderByDesc(Project::getId);
        return projectMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getUserProjectsWithDetails(String userId) {
        List<Project> projects = getUserProjects(userId);
        return projects.stream().map(project -> {
            Map<String, Object> map = new HashMap<>();
            map.put("key", project.getProjectId());
            map.put("name", project.getProjectName());
            map.put("pid", project.getId());
            return map;
        }).collect(Collectors.toList());
    }

    public Project addProject(String userId, String projectId, String projectName) {
        if (userId == null || StrUtil.isBlank(projectId)) {
            throw new BusinessException("User ID and Project ID are required");
        }

        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId);
        wrapper.eq(Project::getKey, projectId);
        Project existing = projectMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException("Project already exists in user list");
        }

        Project project = new Project();
        project.setUserId(userId);
        project.setProjectId(projectId);
        project.setProjectName(StrUtil.isBlank(projectName) ? projectId : projectName);
        project.setViewMode("iteration");
        projectMapper.insert(project);
        return project;
    }

    public boolean removeProject(String userId, Long projectId) {
        if (userId == null || projectId == null) {
            throw new BusinessException("User ID and Project ID are required");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("Project not found");
        }

        if (!project.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this project");
        }

        return projectMapper.deleteById(projectId) > 0;
    }

    public boolean updateViewMode(String userId, Long projectId, String viewMode) {
        if (userId == null || projectId == null) {
            throw new BusinessException("User ID and Project ID is required");
        }

        if (StrUtil.isBlank(viewMode)) {
            viewMode = "iteration";
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("Project not found");
        }

        if (!project.getUserId().equals(userId)) {
            throw new BusinessException("No permission to update this project");
        }

        project.setViewMode(viewMode);
        return projectMapper.updateById(project) > 0;
    }

    public Project getProjectById(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("Project ID is required");
        }
        return projectMapper.selectById(projectId);
    }
}