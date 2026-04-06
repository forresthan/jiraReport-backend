package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_record")
public class TaskRecord extends BaseEntity {

    private String taskType;

    private String taskKey;

    private Long userId;

    private String status;

    @TableField("`result`")
    private String result;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
