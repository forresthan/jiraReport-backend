package com.jirareport.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.util.JwtUtil;
import com.jirareport.mapper.UserInfoMapper;
import com.jirareport.mapper.entity.UserInfo;
import com.jirareport.service.IUserService;
import com.jirareport.service.JiraService;
import com.jirareport.service.QcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final JiraService jiraService;
    private final QcService qcService;
    private final UserInfoMapper userInfoMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String JIRA_SESSION_PREFIX = "jira:session:";
    private static final Duration USER_CACHE_DURATION = Duration.ofDays(7);
    private static final Duration SESSION_DURATION = Duration.ofHours(8);

    public UserServiceImpl(JiraService jiraService, QcService qcService, 
                          UserInfoMapper userInfoMapper, StringRedisTemplate redisTemplate,
                          JwtUtil jwtUtil) {
        this.jiraService = jiraService;
        this.qcService = qcService;
        this.userInfoMapper = userInfoMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        JSONObject jiraUser = jiraService.getUser(username);
        if (jiraUser == null) {
            throw new RuntimeException("无法获取JIRA用户信息");
        }

        Map<String, Object> qcUser = null;
        try {
            qcUser = qcService.getUserByUsername(username);
        } catch (Exception e) {
            log.warn("QC系统用户信息获取失败: {}", username, e);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", jiraUser.getStr("key"));
        userInfo.put("name", username);
        userInfo.put("displayName", jiraUser.getStr("displayName", username));
        userInfo.put("email", jiraUser.getStr("emailAddress"));

        if (qcUser != null) {
            userInfo.put("erpId", qcUser.get("erpId"));
            userInfo.put("fullname", qcUser.get("fullname"));
            userInfo.put("department", qcUser.get("department"));
            userInfo.put("phone", qcUser.get("phone"));
            userInfo.put("internal", qcUser.get("internal"));
        }

        userInfo.put("access", computeAccess(userInfo));

        String userKey = USER_CACHE_PREFIX + userInfo.get("id");
        redisTemplate.opsForValue().set(userKey, JSONUtil.toJsonStr(userInfo), USER_CACHE_DURATION);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userInfo.get("id"));
        String token = jwtUtil.generateToken(username, claims);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        return result;
    }

    @Override
    public void logout(String username) {
        try {
            JSONObject jiraUser = jiraService.getUser(username);
            if (jiraUser != null) {
                String userId = jiraUser.getStr("key");
                redisTemplate.delete(USER_CACHE_PREFIX + userId);
            }
        } catch (Exception e) {
            log.warn("登出时清理缓存失败: {}", username, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String username) {
        try {
            JSONObject jiraUser = jiraService.getUser(username);
            if (jiraUser != null) {
                String userId = jiraUser.getStr("key");
                String cached = redisTemplate.opsForValue().get(USER_CACHE_PREFIX + userId);
                if (StrUtil.isNotBlank(cached)) {
                    return JSONUtil.toBean(cached, Map.class);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", username, e);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) login(username, "").get("userInfo");
        return result;
    }

    public void setGitlabToken(String userId, String gitlabToken) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);

        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setGitlabToken(gitlabToken);
            userInfoMapper.insert(userInfo);
        } else {
            userInfo.setGitlabToken(gitlabToken);
            userInfoMapper.updateById(userInfo);
        }
    }

    public String getGitlabToken(String userId) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUserId, userId);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        return userInfo != null ? userInfo.getGitlabToken() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> computeAccess(Map<String, Object> userInfo) {
        Map<String, Boolean> access = new HashMap<>();
        
        Object internalObj = userInfo.get("internal");
        Integer internal = null;
        if (internalObj instanceof Number) {
            internal = ((Number) internalObj).intValue();
        }

        if (internal != null && internal == 1) {
            access.put("admin", true);
            access.put("partner", false);
        } else if (internal != null && internal == 0) {
            access.put("admin", false);
            access.put("partner", true);
        } else {
            access.put("admin", false);
            access.put("partner", true);
        }

        return access;
    }

    @Override
    public void storeJiraSession(String userId, String jiraSession) {
        String sessionKey = JIRA_SESSION_PREFIX + userId;
        redisTemplate.opsForValue().set(sessionKey, jiraSession, SESSION_DURATION);
        log.info("JIRA Session已存储 for user: {}", userId);
    }

    @Override
    public String getJiraSession(String userId) {
        String sessionKey = JIRA_SESSION_PREFIX + userId;
        return redisTemplate.opsForValue().get(sessionKey);
    }

    @Override
    public void removeJiraSession(String userId) {
        String sessionKey = JIRA_SESSION_PREFIX + userId;
        redisTemplate.delete(sessionKey);
        log.info("JIRA Session已删除 for user: {}", userId);
    }
}