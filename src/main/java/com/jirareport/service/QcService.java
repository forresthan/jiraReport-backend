package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class QcService {

    private static final Logger log = LoggerFactory.getLogger(QcService.class);

    private static final String QC_COOKIE_KEY = "qc:cookie";
    private static final String QC_TOKEN_KEY = "qc:token";
    private static final long COOKIE_EXPIRE_SECONDS = 3600;

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${qc.base-url}")
    private String baseUrl;

    @Value("${qc.app-id}")
    private String appId;

    @Value("${qc.app-secret}")
    private String appSecret;

    public QcService(StringRedisTemplate stringRedisTemplate) {
        this.restTemplate = new RestTemplate();
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Map<String, Object> login() {
        try {
            String loginUrl = baseUrl + "/oauth2/authorize";
            String redirectUri = baseUrl + "/callback";

            Map<String, String> params = new HashMap<>();
            params.put("app_id", appId);
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "app_id=" + appId + "&redirect_uri=" + redirectUri + "&response_type=code";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    loginUrl + "?app_id=" + appId + "&redirect_uri=" + redirectUri + "&response_type=code",
                    HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = response.getBody();
                log.info("QC login initiated successfully");
                return result != null ? result : new HashMap<>();
            }

            log.warn("QC login returned status: {}", response.getStatusCode());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("QC login failed: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public String getCookie() {
        String cachedCookie = stringRedisTemplate.opsForValue().get(QC_COOKIE_KEY);
        if (StrUtil.isNotBlank(cachedCookie)) {
            log.debug("Using cached QC cookie");
            return cachedCookie;
        }

        return refreshCookie();
    }

    public String refreshCookie() {
        try {
            String tokenUrl = baseUrl + "/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "app_id=" + appId + "&app_secret=" + appSecret + "&grant_type=client_credentials";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                String accessToken = (String) tokenResponse.get("access_token");
                String cookie = (String) tokenResponse.get("cookie");

                if (StrUtil.isNotBlank(accessToken)) {
                    stringRedisTemplate.opsForValue().set(QC_TOKEN_KEY, accessToken, COOKIE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                }

                if (StrUtil.isNotBlank(cookie)) {
                    stringRedisTemplate.opsForValue().set(QC_COOKIE_KEY, cookie, COOKIE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    log.info("QC cookie refreshed successfully");
                    return cookie;
                }
            }

            log.warn("QC token/cookie refresh returned empty response");
            return "";
        } catch (Exception e) {
            log.error("Failed to refresh QC cookie: {}", e.getMessage(), e);
            return "";
        }
    }

    public List<Map<String, Object>> getUser(String keyword) {
        try {
            String cookie = getCookie();
            if (StrUtil.isBlank(cookie)) {
                log.warn("No QC cookie available, cannot fetch users");
                return new ArrayList<>();
            }

            String url = baseUrl + "/api/users?keyword=" + (keyword != null ? keyword : "");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", cookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> users = response.getBody();
                log.info("Fetched {} users from QC", users != null ? users.size() : 0);
                return users != null ? users : new ArrayList<>();
            }

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("QC session expired, attempting to refresh");
                String newCookie = refreshCookie();
                if (StrUtil.isNotBlank(newCookie)) {
                    return getUser(keyword);
                }
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get QC users: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getUserByUsername(String username) {
        try {
            String cookie = getCookie();
            if (StrUtil.isBlank(cookie)) {
                log.warn("No QC cookie available, cannot fetch user");
                return new HashMap<>();
            }

            String url = baseUrl + "/api/user?username=" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", cookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> user = response.getBody();
                log.info("Fetched user {} from QC", username);
                return user != null ? user : new HashMap<>();
            }

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("QC session expired, attempting to refresh");
                String newCookie = refreshCookie();
                if (StrUtil.isNotBlank(newCookie)) {
                    return getUserByUsername(username);
                }
            }

            return new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to get QC user by username: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public List<Map<String, Object>> getMyPartner() {
        try {
            String cookie = getCookie();
            if (StrUtil.isBlank(cookie)) {
                log.warn("No QC cookie available, cannot fetch partners");
                return new ArrayList<>();
            }

            String url = baseUrl + "/api/user/partners";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", cookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> partners = response.getBody();
                log.info("Fetched {} partners from QC", partners != null ? partners.size() : 0);
                return partners != null ? partners : new ArrayList<>();
            }

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("QC session expired, attempting to refresh");
                String newCookie = refreshCookie();
                if (StrUtil.isNotBlank(newCookie)) {
                    return getMyPartner();
                }
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get QC partners: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Map<String, String> refreshFixedIds() {
        Map<String, String> result = new HashMap<>();
        try {
            String cookie = getCookie();
            if (StrUtil.isBlank(cookie)) {
                result.put("status", "failed");
                result.put("message", "No QC cookie available");
                return result;
            }

            String url = baseUrl + "/api/fixed-ids/refresh";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", cookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                result.put("status", "success");
                result.put("message", "Fixed IDs refreshed successfully");
                log.info("QC fixed IDs refreshed");
                return result;
            }

            result.put("status", "failed");
            result.put("message", "Refresh returned non-OK status");
            return result;
        } catch (Exception e) {
            log.error("Failed to refresh QC fixed IDs: {}", e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getCurrentUser() {
        try {
            String cookie = getCookie();
            if (StrUtil.isBlank(cookie)) {
                log.warn("No QC cookie available, cannot fetch current user");
                return new HashMap<>();
            }

            String url = baseUrl + "/api/user/current";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", cookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> user = response.getBody();
                log.info("Fetched current QC user");
                return user != null ? user : new HashMap<>();
            }

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("QC session expired, attempting to refresh");
                String newCookie = refreshCookie();
                if (StrUtil.isNotBlank(newCookie)) {
                    return getCurrentUser();
                }
            }

            return new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to get current QC user: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}
