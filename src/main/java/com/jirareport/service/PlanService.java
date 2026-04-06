package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jirareport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private final JiraService jiraService;
    private final IUserService userService;

    @Value("${jira.project-key:}")
    private String defaultProjectKey;

    public PlanService(JiraService jiraService, IUserService userService) {
        this.jiraService = jiraService;
        this.userService = userService;
    }

    public enum ViewMode {
        ITERATION("iteration"),
        PRODUCT("product"),
        REQUIREMENT("requirement");

        private final String value;

        ViewMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public List<Map<String, Object>> getIssues(String userId, String projectKey, String viewMode, String sprintId) {
        String actualProjectKey = StrUtil.isBlank(projectKey) ? defaultProjectKey : projectKey;
        String jql = buildJql(actualProjectKey, viewMode, sprintId);
        String session = userService.getJiraSession(userId);
        return jiraService.searchIssues(jql, 0, 100, session);
    }

    private String buildJql(String projectKey, String viewMode, String sprintId) {
        StringBuilder jql = new StringBuilder("project = ").append(projectKey);

        if (StrUtil.isNotBlank(sprintId)) {
            if ("iteration".equals(viewMode)) {
                jql.append(" AND sprint = ").append(sprintId);
            } else if ("product".equals(viewMode)) {
                jql.append(" AND fixVersion = ").append(sprintId);
            }
        }

        jql.append(" ORDER BY created DESC");
        return jql.toString();
    }

    public List<Map<String, Object>> getVersions(String userId, String projectKey) {
        String actualProjectKey = StrUtil.isBlank(projectKey) ? defaultProjectKey : projectKey;
        String session = userService.getJiraSession(userId);
        return jiraService.getVersions(actualProjectKey, session);
    }

    public boolean updatePriority(String issueKey, String priority) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("priority", new JSONObject().set("name", priority));
        return jiraService.updateIssue(issueKey, fields);
    }

    public boolean updateLabels(String issueKey, List<String> labels) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("labels", labels);
        return jiraService.updateIssue(issueKey, fields);
    }

    public boolean updateOrder(String issueKey, int order) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("customfield_10001", order);
        return jiraService.updateIssue(issueKey, fields);
    }

    public boolean updateFields(String issueKey, Map<String, Object> customFields) {
        return jiraService.updateIssue(issueKey, customFields);
    }

    public boolean updateFixVersions(String issueKey, List<String> versions) {
        List<Map<String, String>> fixVersions = versions.stream()
                .map(v -> {
                    Map<String, String> version = new HashMap<>();
                    version.put("name", v);
                    return version;
                })
                .collect(Collectors.toList());

        Map<String, Object> fields = new HashMap<>();
        fields.put("fixVersions", fixVersions);
        return jiraService.updateIssue(issueKey, fields);
    }

    public List<Map<String, Object>> getSubtasks(String issueKey) {
        Map<String, Object> issue = jiraService.getIssue(issueKey);
        if (issue == null) {
            return Collections.emptyList();
        }

        Object subtasksObj = issue.get("fields");
        if (subtasksObj == null) {
            return Collections.emptyList();
        }

        Map<String, Object> fields = (Map<String, Object>) subtasksObj;
        Object subtasks = fields.get("subtasks");

        if (subtasks instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) subtasks;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : jsonArray) {
                result.add(JSONUtil.parseObj(JSONUtil.toJsonStr(item)));
            }
            return result;
        } else if (subtasks instanceof List) {
            return (List<Map<String, Object>>) subtasks;
        }
        return Collections.emptyList();
    }

    public boolean saveSubtasks(String issueKey, List<Map<String, Object>> subtasks) {
        for (Map<String, Object> subtask : subtasks) {
            String subtaskKey = (String) subtask.get("key");
            if (StrUtil.isNotBlank(subtaskKey)) {
                Map<String, Object> fields = new HashMap<>();
                Object summary = subtask.get("summary");
                Object status = subtask.get("status");
                Object assignee = subtask.get("assignee");

                if (summary != null) {
                    fields.put("summary", summary);
                }
                if (status != null) {
                    fields.put("status", new JSONObject().set("name", status));
                }
                if (assignee != null) {
                    fields.put("assignee", new JSONObject().set("name", assignee));
                }

                if (!fields.isEmpty()) {
                    jiraService.updateIssue(subtaskKey, fields);
                }
            }
        }
        return true;
    }

    public List<Map<String, Object>> getComment(String issueKey) {
        return jiraService.getComments(issueKey);
    }

    public boolean updateComment(String issueKey, String commentId, String content) {
        return jiraService.updateComment(issueKey, commentId, content);
    }

    public Map<String, Object> computedPositionStat(String userId, String projectKey, String viewMode) {
        List<Map<String, Object>> issues = getIssues(userId, projectKey, viewMode, null);

        Map<String, List<Map<String, Object>>> assigneeIssues = issues.stream()
                .filter(issue -> {
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    return fields != null && fields.get("assignee") != null;
                })
                .collect(Collectors.groupingBy(issue -> {
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                    return (String) assignee.get("displayName");
                }));

        Map<String, Object> stat = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : assigneeIssues.entrySet()) {
            Map<String, Object> userStat = new HashMap<>();
            userStat.put("total", entry.getValue().size());

            long doneCount = entry.getValue().stream()
                    .filter(issue -> {
                        Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                        Map<String, Object> status = (Map<String, Object>) fields.get("status");
                        return "Done".equals(status.get("name")) || "Closed".equals(status.get("name"));
                    })
                    .count();
            userStat.put("done", doneCount);
            userStat.put("todo", entry.getValue().size() - doneCount);

            stat.put(entry.getKey(), userStat);
        }

        return stat;
    }

    public List<Map<String, Object>> getPartnerIssues(String userId, String projectKey, String viewMode, List<String> partnerIds) {
        String actualProjectKey = StrUtil.isBlank(projectKey) ? defaultProjectKey : projectKey;

        if (partnerIds == null || partnerIds.isEmpty()) {
            return Collections.emptyList();
        }

        String assigneeCondition = String.join("' OR assignee = '", partnerIds);
        String jql = "project = " + actualProjectKey + " AND (assignee = '" +
                assigneeCondition + "') ORDER BY updated DESC";

        String session = userService.getJiraSession(userId);
        List<Map<String, Object>> issues = jiraService.searchIssues(jql, 0, 100, session);

        Map<String, List<Map<String, Object>>> groupedIssues = issues.stream()
                .collect(Collectors.groupingBy(issue -> {
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                    return (String) assignee.get("accountId");
                }));

        List<Map<String, Object>> result = new ArrayList<>();
        for (String partnerId : partnerIds) {
            Map<String, Object> partnerData = new HashMap<>();
            partnerData.put("partnerId", partnerId);
            partnerData.put("issues", groupedIssues.getOrDefault(partnerId, Collections.emptyList()));
            result.add(partnerData);
        }

        return result;
    }
}