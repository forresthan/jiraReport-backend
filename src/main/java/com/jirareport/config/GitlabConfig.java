package com.jirareport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gitlab")
public class GitlabConfig {

    private String baseUrl;
    private String privateToken;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPrivateToken() {
        return privateToken;
    }

    public void setPrivateToken(String privateToken) {
        this.privateToken = privateToken;
    }
}