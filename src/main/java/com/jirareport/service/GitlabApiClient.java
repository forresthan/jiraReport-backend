package com.jirareport.service;

import com.jirareport.config.GitlabConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
public class GitlabApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitlabApiClient.class);

    private final RestTemplate restTemplate;
    private final GitlabConfig gitlabConfig;

    public GitlabApiClient(GitlabConfig gitlabConfig) {
        this.restTemplate = new RestTemplate();
        this.gitlabConfig = gitlabConfig;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", gitlabConfig.getPrivateToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<Map<String, Object>> getProjects() {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/projects?membership=true&per_page=100";
        List<Map<String, Object>> allProjects = new ArrayList<>();
        int page = 1;

        while (true) {
            String pageUrl = url + "&page=" + page;
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<List> response = restTemplate.exchange(pageUrl, HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> projects = response.getBody();
            if (projects == null || projects.isEmpty()) {
                break;
            }

            allProjects.addAll(projects);
            page++;

            if (projects.size() < 100) {
                break;
            }
        }

        log.info("Fetched {} projects from GitLab", allProjects.size());
        return allProjects;
    }

    public List<Map<String, Object>> getProjectCommits(String projectId, int perPage) {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/projects/" + projectId + "/repository/commits?per_page=" + perPage;
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> commits = response.getBody();
        log.info("Fetched {} commits from project {}", commits != null ? commits.size() : 0, projectId);
        return commits != null ? commits : new ArrayList<>();
    }

    public List<Map<String, Object>> getProjectCommitsWithPagination(String projectId, int page, int perPage) {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/projects/" + projectId + "/repository/commits?page=" + page + "&per_page=" + perPage;
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> commits = response.getBody();
        log.debug("Fetched page {} of commits from project {}, got {} commits", page, projectId, commits != null ? commits.size() : 0);
        return commits != null ? commits : new ArrayList<>();
    }

    public Map<String, Object> getProject(String projectId) {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/projects/" + projectId;
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        return response.getBody();
    }

    public List<Map<String, Object>> getProjectMembers(String projectId) {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/projects/" + projectId + "/members/all";
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> members = response.getBody();
        return members != null ? members : new ArrayList<>();
    }

    public List<Map<String, Object>> getUserProjects(String username) {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/users/" + username + "/projects";
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> projects = response.getBody();
        return projects != null ? projects : new ArrayList<>();
    }

    public boolean checkToken() {
        try {
            String url = gitlabConfig.getBaseUrl() + "/api/v4/user";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getCurrentUser() {
        String url = gitlabConfig.getBaseUrl() + "/api/v4/user";
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        return response.getBody();
    }
}