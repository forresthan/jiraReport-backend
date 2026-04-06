package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jirareport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JiraService {

    private static final Logger log = LoggerFactory.getLogger(JiraService.class);

    @Value("${jira.base-url:}")
    private String jiraBaseUrl;

    @Value("${jira.username:}")
    private String jiraUsername;

    @Value("${jira.password:}")
    private String jiraPassword;

    @Value("${jira.project-key:}")
    private String jiraProjectKey;

    private final RestTemplate restTemplate;

    public JiraService() {
        this.restTemplate = new RestTemplate(createRequestFactory());
    }

    private static ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        return factory;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String auth = jiraUsername + ":" + jiraPassword;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    private HttpHeaders createSessionHeaders(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (StrUtil.isNotBlank(sessionCookie)) {
            headers.set("Cookie", sessionCookie);
            log.debug("Using session cookie for JIRA request");
        }
        return headers;
    }

    public boolean login() {
        if (StrUtil.isBlank(jiraBaseUrl) || StrUtil.isBlank(jiraUsername) || StrUtil.isBlank(jiraPassword)) {
            throw new BusinessException("JIRA configuration is incomplete");
        }
        return login(jiraUsername, jiraPassword);
    }

    public boolean login(String username, String password) {
        if (StrUtil.isBlank(jiraBaseUrl) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException("JIRA username or password is empty");
        }
        String url = jiraBaseUrl + "/rest/api/2/user?username=" + username;
        log.info("Attempting JIRA login for user: {}, URL: {}", username, url);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        RestTemplate restTemplate = new RestTemplate(factory);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            long duration = System.currentTimeMillis() - startTime;
            log.info("JIRA login success for user: {} in {}ms, status: {}", username, duration, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.warn("JIRA authentication failed for user: {}", username);
            return false;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("JIRA connection timeout for user {}: {}", username, e.getMessage());
            throw new BusinessException("JIRA 服务器连接超时，请检查网络或稍后重试");
        } catch (Exception e) {
            log.error("JIRA login failed for user {}: {}", username, e.getMessage(), e);
            throw new BusinessException("JIRA 登录失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getCurrentUserBySession(String sessionCookie) {
        String url = jiraBaseUrl + "/rest/api/2/myself";
        log.info("Getting current user by session, URL: {}", url);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        RestTemplate sessionRestTemplate = new RestTemplate(factory);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Cookie", sessionCookie);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = sessionRestTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                return JSONUtil.toBean(response.getBody(), Map.class);
            }
            throw new BusinessException("无法获取用户信息");
        } catch (Exception e) {
            log.error("Failed to get current user by session: {}", e.getMessage(), e);
            throw new BusinessException("JIRA Session无效或已过期");
        }
    }

    public JSONObject getUser(String username) {
        String url = jiraBaseUrl + "/rest/api/2/user?username=" + username;
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                return JSONUtil.parseObj(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to get JIRA user: {}", username, e);
        }
        return null;
    }

    public List<Map<String, Object>> searchIssues(String jql, int startAt, int maxResults) {
        return searchIssues(jql, startAt, maxResults, null);
    }

    public List<Map<String, Object>> searchIssues(String jql, int startAt, int maxResults, String sessionCookie) {
        String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
        String url = jiraBaseUrl + "/rest/api/2/search?jql=" + encodedJql + "&startAt=" + startAt + "&maxResults=" + maxResults
                + "&fields=summary,description,status,priority,assignee,reporter,created,updated,duedate,labels,components,versions,fixVersions,parent,subtasks,customfield_10001,customfield_10002,customfield_10003";
        log.info("Searching JIRA issues with JQL: {}", jql);
        log.info("Full URL: {}", url);
        log.info("Session cookie present: {}", StrUtil.isNotBlank(sessionCookie));

        try {
            HttpHeaders headers = StrUtil.isNotBlank(sessionCookie) ? createSessionHeaders(sessionCookie) : createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.debug("Sending GET request to JIRA search API");
            long startTime = System.currentTimeMillis();
            URI uri = URI.create(url);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            long duration = System.currentTimeMillis() - startTime;
            log.info("JIRA search success in {}ms, status: {}", duration, response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONObject result = JSONUtil.parseObj(response.getBody());
                JSONArray issues = result.getJSONArray("issues");
                List<Map<String, Object>> list = new ArrayList<>();
                for (int i = 0; i < issues.size(); i++) {
                    list.add(convertJsonToMap(issues.getJSONObject(i)));
                }
                log.info("Found {} issues", list.size());
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to search JIRA issues: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public Map<String, Object> getIssue(String issueKey) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey;
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                return JSONUtil.toBean(response.getBody(), Map.class);
            }
        } catch (Exception e) {
            log.error("Failed to get JIRA issue: {}", issueKey, e);
        }
        return null;
    }

    public boolean updateIssue(String issueKey, Map<String, Object> fields) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey;
        JSONObject body = new JSONObject();
        body.set("fields", fields);
        try {
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to update JIRA issue: {}", issueKey, e);
            return false;
        }
    }

    public List<Map<String, Object>> getComments(String issueKey) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "/comment";
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONObject result = JSONUtil.parseObj(response.getBody());
                JSONArray comments = result.getJSONArray("comments");
                List<Map<String, Object>> list = new ArrayList<>();
                for (int i = 0; i < comments.size(); i++) {
                    list.add(comments.get(i, Map.class));
                }
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to get comments for issue: {}", issueKey, e);
        }
        return new ArrayList<>();
    }

    public boolean updateComment(String issueKey, String commentId, String content) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "/comment/" + commentId;
        JSONObject body = new JSONObject();
        body.set("body", content);
        try {
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to update comment {} for issue: {}", commentId, issueKey, e);
            return false;
        }
    }

    public List<Map<String, Object>> getVersions(String projectKey) {
        return getVersions(projectKey, null);
    }

    public List<Map<String, Object>> getVersions(String projectKey, String sessionCookie) {
        String url = jiraBaseUrl + "/rest/api/2/project/" + projectKey + "/versions";
        try {
            HttpHeaders headers = StrUtil.isNotBlank(sessionCookie) ? createSessionHeaders(sessionCookie) : createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONArray versions = JSONUtil.parseArray(response.getBody());
                List<Map<String, Object>> list = new ArrayList<>();
                for (int i = 0; i < versions.size(); i++) {
                    list.add(versions.get(i, Map.class));
                }
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to get JIRA versions for project: {}", projectKey, e);
        }
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getProjects() {
        String url = jiraBaseUrl + "/rest/api/2/project";
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONArray projects = JSONUtil.parseArray(response.getBody());
                List<Map<String, Object>> list = new ArrayList<>();
                for (int i = 0; i < projects.size(); i++) {
                    list.add(projects.get(i, Map.class));
                }
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to get JIRA projects", e);
        }
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getProjectsBySession(String sessionCookie) {
        String url = jiraBaseUrl + "/rest/api/2/project";
        try {
            HttpHeaders headers = createSessionHeaders(sessionCookie);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONArray projects = JSONUtil.parseArray(response.getBody());
                List<Map<String, Object>> list = new ArrayList<>();
                for (int i = 0; i < projects.size(); i++) {
                    JSONObject projectJson = projects.getJSONObject(i);
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("key", projectJson.getStr("key"));
                    map.put("name", projectJson.getStr("name"));
                    map.put("id", projectJson.get("id"));
                    list.add(map);
                }
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to get JIRA projects by session", e);
        }
        return new ArrayList<>();
    }

    private Map<String, Object> convertJsonToMap(JSONObject jsonObject) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONNull) {
                map.put(key, null);
            } else if (value instanceof JSONObject) {
                map.put(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, convertJsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private List<Object> convertJsonArrayToList(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONNull) {
                list.add(null);
            } else if (value instanceof JSONObject) {
                list.add(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(convertJsonArrayToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }
        return list;
    }
}