package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("jira_comment")
public class JiraComment extends BaseEntity {

    private String issueKey;

    private String commentId;

    private LocalDateTime createdAt;
}
