package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.mapper.RobotConfigMapper;
import com.jirareport.mapper.entity.RobotConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RobotService {

    private static final Logger log = LoggerFactory.getLogger(RobotService.class);

    private final RobotConfigMapper robotConfigMapper;
    private final JiraService jiraService;
    private final RestTemplate restTemplate;

    @Value("${robot.webhook-url:}")
    private String defaultWebhookUrl;

    @Value("${robot.secret:}")
    private String defaultSecret;

    public RobotService(RobotConfigMapper robotConfigMapper, JiraService jiraService) {
        this.robotConfigMapper = robotConfigMapper;
        this.jiraService = jiraService;
        this.restTemplate = new RestTemplate();
    }

    public List<RobotConfig> getConfigs() {
        return robotConfigMapper.selectList(null);
    }

    public RobotConfig saveConfig(RobotConfig config) {
        if (config.getId() == null) {
            robotConfigMapper.insert(config);
        } else {
            robotConfigMapper.updateById(config);
        }
        return config;
    }

    public boolean deleteConfig(Long id) {
        return robotConfigMapper.deleteById(id) > 0;
    }

    public Map<String, Object> doRemindBS() {
        List<Map<String, Object>> pendingRequirements = queryPendingRequirements();

        if (pendingRequirements.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", 0);
            result.put("sentCount", 0);
            result.put("groups", Collections.emptyList());
            return result;
        }

        Map<String, List<Map<String, Object>>> groupedByReporter = pendingRequirements.stream()
                .collect(Collectors.groupingBy(item -> {
                    Object reporter = item.get("reporter");
                    return reporter != null ? reporter.toString() : "unknown";
                }));

        List<RobotConfig> enabledConfigs = robotConfigMapper.selectList(
                new LambdaQueryWrapper<RobotConfig>().eq(RobotConfig::getEnabled, true)
        );

        if (enabledConfigs.isEmpty()) {
            enabledConfigs.add(buildDefaultConfig());
        }

        int sentCount = 0;
        List<Map<String, Object>> groupResults = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByReporter.entrySet()) {
            String reporter = entry.getKey();
            List<Map<String, Object>> issues = entry.getValue();

            String markdownContent = buildBSReminderMarkdown(reporter, issues);

            for (RobotConfig config : enabledConfigs) {
                boolean sent = sendMarkdown(config, markdownContent);
                if (sent) {
                    sentCount++;
                    for (Map<String, Object> issue : issues) {
                        incrementRemindCount((String) issue.get("key"));
                    }
                }
            }

            Map<String, Object> groupResult = new HashMap<>();
            groupResult.put("reporter", reporter);
            groupResult.put("count", issues.size());
            groupResults.add(groupResult);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", pendingRequirements.size());
        result.put("sentCount", sentCount);
        result.put("groups", groupResults);
        return result;
    }

    private List<Map<String, Object>> queryPendingRequirements() {
        String jql = "type = \"业务需求\" AND status = \"待确认\" ORDER BY created DESC";
        return jiraService.searchIssues(jql, 0, 100);
    }

    private String buildBSReminderMarkdown(String reporter, List<Map<String, Object>> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📢 业务需求确认提醒\n\n");
        sb.append("**需求提出人**: ").append(reporter).append("\n\n");
        sb.append("**待确认需求共 ").append(issues.size()).append(" 条**:\n\n");

        sb.append("| 需求Key | 摘要 | 状态 | 创建时间 |\n");
        sb.append("|---------|------|------|----------|\n");

        for (Map<String, Object> issue : issues) {
            String key = (String) issue.get("key");
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            String summary = fields != null ? (String) fields.get("summary") : "";
            String status = fields != null ? (String) ((Map<String, Object>) fields.get("status")).get("name") : "";
            String created = fields != null ? (String) fields.get("created") : "";

            sb.append("| ").append(key).append(" | ").append(summary).append(" | ").append(status).append(" | ").append(created).append(" |\n");
        }

        sb.append("\n请尽快确认需求状态，谢谢！");
        return sb.toString();
    }

    public boolean sendMarkdown(RobotConfig config, String content) {
        JSONObject body = new JSONObject();
        body.set("msgtype", "markdown");

        JSONObject markdown = new JSONObject();
        markdown.set("content", content);
        body.set("markdown", markdown);

        return send(config, body);
    }

    private boolean send(RobotConfig config, JSONObject body) {
        String webhookUrl = getWebhookUrl(config);
        String secret = getSecret(config);

        if (StrUtil.isBlank(webhookUrl)) {
            log.warn("Robot webhook URL is empty, skip sending");
            return false;
        }

        try {
            String urlWithSign = addSign(webhookUrl, secret);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(urlWithSign, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Robot message sent successfully");
                return true;
            } else {
                log.error("Robot message send failed: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Robot message send error: {}", e.getMessage());
            return false;
        }
    }

    private String addSign(String webhookUrl, String secret) {
        if (StrUtil.isBlank(secret)) {
            return webhookUrl;
        }

        try {
            long timestamp = Instant.now().getEpochSecond() * 1000;
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(signData);

            return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("Generate signature failed: {}", e.getMessage());
            return webhookUrl;
        }
    }

    private String getWebhookUrl(RobotConfig config) {
        if (StrUtil.isNotBlank(config.getConfig())) {
            JSONObject configJson = JSONUtil.parseObj(config.getConfig());
            String webhookUrl = configJson.getStr("webhook-url");
            if (StrUtil.isNotBlank(webhookUrl)) {
                return webhookUrl;
            }
        }
        return defaultWebhookUrl;
    }

    private String getSecret(RobotConfig config) {
        if (StrUtil.isNotBlank(config.getConfig())) {
            JSONObject configJson = JSONUtil.parseObj(config.getConfig());
            String secret = configJson.getStr("secret");
            if (StrUtil.isNotBlank(secret)) {
                return secret;
            }
        }
        return defaultSecret;
    }

    private RobotConfig buildDefaultConfig() {
        RobotConfig config = new RobotConfig();
        config.setRobotType("wechat");
        config.setEnabled(true);
        return config;
    }

    private void incrementRemindCount(String issueKey) {
        try {
            Map<String, Object> issue = jiraService.getIssue(issueKey);
            if (issue != null) {
                Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                Object customField = fields.get("customfield_10003");
                int currentCount = customField != null ? Integer.parseInt(customField.toString()) : 0;

                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("customfield_10003", currentCount + 1);
                jiraService.updateIssue(issueKey, updateFields);
            }
        } catch (Exception e) {
            log.error("Increment remind count failed for issue: {}", issueKey, e);
        }
    }
}