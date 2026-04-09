package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ppt_export_job")
public class PptExportJob extends BaseEntity {

    @TableField("owner_id")
    private String ownerId;

    @TableField("report_id")
    private Long reportId;

    @TableField("theme_key")
    private String themeKey;

    private String status;

    private Integer progress;

    @TableField("result_url")
    private String resultUrl;

    @TableField("error_message")
    private String errorMessage;
}
