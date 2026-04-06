package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {

    private String userId;

    private String gitlabToken;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGitlabToken() {
        return gitlabToken;
    }

    public void setGitlabToken(String gitlabToken) {
        this.gitlabToken = gitlabToken;
    }
}
