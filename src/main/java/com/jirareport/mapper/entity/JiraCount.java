package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("jira_count")
public class JiraCount extends BaseEntity {

    private String countType;

    private String countKey;

    private Integer countValue;

    private LocalDate date;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
