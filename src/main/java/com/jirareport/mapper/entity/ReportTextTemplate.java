package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_text_template")
public class ReportTextTemplate extends BaseEntity {

    @TableField("owner_id")
    private String ownerId;

    @TableField("project_key")
    private String projectKey;

    private String name;

    private Integer enabled;

    @TableField("sections_json")
    private String sectionsJson;
}
