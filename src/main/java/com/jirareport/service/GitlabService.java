package com.jirareport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.mapper.GitlabDataMapper;
import com.jirareport.mapper.UserMapper;
import com.jirareport.mapper.entity.GitlabData;
import com.jirareport.mapper.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitlabService {

    private static final Logger log = LoggerFactory.getLogger(GitlabService.class);

    private final GitlabApiClient gitlabApiClient;
    private final GitlabDataMapper gitlabDataMapper;
    private final UserMapper userMapper;

    public GitlabService(GitlabApiClient gitlabApiClient, GitlabDataMapper gitlabDataMapper, UserMapper userMapper) {
        this.gitlabApiClient = gitlabApiClient;
        this.gitlabDataMapper = gitlabDataMapper;
        this.userMapper = userMapper;
    }

    public List<Map<String, Object>> getProjects() {
        return gitlabApiClient.getProjects();
    }

    public List<Map<String, Object>> getProjectCommits(String projectId, int perPage) {
        return gitlabApiClient.getProjectCommits(projectId, perPage);
    }

    public List<Map<String, Object>> getProjectCommitsLoop(String projectId, int perPage) {
        List<Map<String, Object>> allCommits = new ArrayList<>();
        int page = 1;
        List<Map<String, Object>> commits;

        do {
            commits = gitlabApiClient.getProjectCommitsWithPagination(projectId, page, perPage);
            allCommits.addAll(commits);
            page++;
        } while (!commits.isEmpty());

        log.info("Fetched total {} commits from project {} via pagination", allCommits.size(), projectId);
        return allCommits;
    }

    public String syncGitlabData(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("User not found: {}", userId);
            return "User not found";
        }

        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            username = user.getEmail().split("@")[0];
        }

        log.info("Starting GitLab data sync for user: {}", username);
        return syncGitlabDataByUser(username);
    }

    public String syncGitlabDataByUser(String username) {
        try {
            List<Map<String, Object>> projects = gitlabApiClient.getUserProjects(username);
            if (projects.isEmpty()) {
                log.info("No projects found for user: {}", username);
                return "No projects found for user";
            }

            int totalCommits = 0;
            int savedCommits = 0;

            for (Map<String, Object> project : projects) {
                String projectId = String.valueOf(project.get("id"));
                String projectName = (String) project.get("name");

                List<Map<String, Object>> commits = gitlabApiClient.getProjectCommits(projectId, 100);
                totalCommits += commits.size();

                for (Map<String, Object> commit : commits) {
                    saveCommitData(projectId, projectName, commit);
                    savedCommits++;
                }
            }

            log.info("Synced {} commits from {} projects for user {}", savedCommits, projects.size(), username);
            return String.format("Synced %d commits from %d projects", savedCommits, projects.size());
        } catch (Exception e) {
            log.error("Error syncing GitLab data for user {}: {}", username, e.getMessage(), e);
            return "Sync failed: " + e.getMessage();
        }
    }

    public String syncAllGitlabData() {
        try {
            List<Map<String, Object>> projects = gitlabApiClient.getProjects();
            if (projects.isEmpty()) {
                log.info("No projects found");
                return "No projects found";
            }

            int totalProjects = projects.size();
            int totalCommits = 0;
            int savedCommits = 0;

            for (Map<String, Object> project : projects) {
                String projectId = String.valueOf(project.get("id"));
                String projectName = (String) project.get("name");

                List<Map<String, Object>> commits = gitlabApiClient.getProjectCommits(projectId, 100);
                totalCommits += commits.size();

                for (Map<String, Object> commit : commits) {
                    saveCommitData(projectId, projectName, commit);
                    savedCommits++;
                }
            }

            log.info("Full sync completed: {} commits from {} projects, saved {} records",
                    totalCommits, totalProjects, savedCommits);
            return String.format("Full sync completed: %d commits from %d projects, saved %d records",
                    totalCommits, totalProjects, savedCommits);
        } catch (Exception e) {
            log.error("Error during full GitLab sync: {}", e.getMessage(), e);
            return "Full sync failed: " + e.getMessage();
        }
    }

    private void saveCommitData(String projectId, String projectName, Map<String, Object> commit) {
        String commitId = (String) commit.get("id");
        String shortId = (String) commit.get("short_id");
        String title = (String) commit.get("title");
        String authorName = (String) commit.get("author_name");
        String authorEmail = (String) commit.get("author_email");
        String committedDateStr = (String) commit.get("committed_date");

        LambdaQueryWrapper<GitlabData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GitlabData::getCommitId, commitId);
        wrapper.eq(GitlabData::getProjectId, projectId);

        GitlabData existing = gitlabDataMapper.selectOne(wrapper);
        if (existing != null) {
            return;
        }

        GitlabData gitlabData = new GitlabData();
        gitlabData.setCommitId(commitId);
        gitlabData.setProjectId(projectId);
        gitlabData.setProjectName(projectName);
        gitlabData.setAuthor(authorEmail);
        gitlabData.setAuthorName(authorName);
        gitlabData.setTitle(title);

        if (committedDateStr != null) {
            try {
                LocalDateTime date = LocalDateTime.parse(committedDateStr.replace("Z", ""));
                gitlabData.setDate(date);
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", committedDateStr);
            }
        }

        gitlabDataMapper.insert(gitlabData);
    }

    public Map<String, Object> getStatByUser(String username, String project, String startDate, String endDate) {
        LambdaQueryWrapper<GitlabData> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(GitlabData::getAuthor, username);

        if (project != null && !project.isEmpty()) {
            wrapper.eq(GitlabData::getProjectName, project);
        }

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(GitlabData::getDate, startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(GitlabData::getDate, endDate);
        }

        List<GitlabData> commits = gitlabDataMapper.selectList(wrapper);

        Map<String, Object> stat = new HashMap<>();
        stat.put("totalCommits", commits.size());

        int totalAdditions = commits.stream().mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0).sum();
        int totalDeletions = commits.stream().mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0).sum();

        stat.put("totalAdditions", totalAdditions);
        stat.put("totalDeletions", totalDeletions);
        stat.put("netChanges", totalAdditions - totalDeletions);

        if (project == null || project.isEmpty()) {
            Map<String, Long> projectStat = commits.stream()
                    .collect(Collectors.groupingBy(GitlabData::getProjectName, Collectors.counting()));
            stat.put("projects", projectStat);
        }

        return stat;
    }

    public Map<String, Object> getStatByUserId(Long userId, String project, String startDate, String endDate) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyMap();
        }

        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            username = user.getEmail().split("@")[0];
        }

        return getStatByUser(username, project, startDate, endDate);
    }

    public Map<String, Object> getStatByUser(String username) {
        return getStatByUser(username, null, null, null);
    }

    public Map<String, Object> getStatByUserId(Long userId) {
        return getStatByUserId(userId, null, null, null);
    }

    public boolean checkToken() {
        return gitlabApiClient.checkToken();
    }

    public Map<String, Object> getCurrentUser() {
        return gitlabApiClient.getCurrentUser();
    }
}