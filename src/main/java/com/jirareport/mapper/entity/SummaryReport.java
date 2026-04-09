package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("summary_report")
public class SummaryReport extends BaseEntity {

    @TableField("owner_id")
    private String ownerId;

    @TableField("project_key")
    private String projectKey;

    @TableField("project_name")
    private String projectName;

    private String title;

    private String content;

    private String status;

    @TableField("template_id")
    private Long templateId;

    @TableField("slides_json")
    private String slidesJson;
}
