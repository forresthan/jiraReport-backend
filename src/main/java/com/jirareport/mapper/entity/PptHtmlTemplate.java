package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ppt_html_template")
public class PptHtmlTemplate extends BaseEntity {

    @TableField("owner_id")
    private String ownerId;

    private String name;

    private String description;

    @TableField("html_content")
    private String htmlContent;

    @TableField("meta_json")
    private String metaJson;

    @TableField("original_file_name")
    private String originalFileName;

    private String status;
}
