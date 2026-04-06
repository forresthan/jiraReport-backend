package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {

    private Long id;

    @TableField("user")
    private String userId;

    private Integer pid;

    @TableField("`key`")
    private String key;

    private String name;

    private Integer mode;

    @TableField("updateAt")
    private LocalDateTime updateAt;

    public String getProjectId() {
        return key;
    }

    public String getProjectName() {
        return name;
    }

    public String getViewMode() {
        return mode != null ? String.valueOf(mode) : "0";
    }

    public void setProjectId(String projectId) {
        this.key = projectId;
    }

    public void setProjectName(String projectName) {
        this.name = projectName;
    }

    public void setViewMode(String viewMode) {
        this.mode = "iteration".equals(viewMode) ? 0 : ("product".equals(viewMode) ? 1 : 2);
    }
}
