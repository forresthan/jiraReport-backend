package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("access_rule")
public class AccessRule extends BaseEntity {

    private String ruleKey;

    @TableField("rule_args")
    private String ruleArgs;

    private LocalDateTime createdAt;
}
