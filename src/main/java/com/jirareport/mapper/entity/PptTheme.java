package com.jirareport.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ppt_theme")
public class PptTheme extends BaseEntity {

    @TableField("theme_key")
    private String themeKey;

    private String name;

    private String style;

    @TableField("preview_meta")
    private String previewMeta;

    @TableField("sort_order")
    private Integer sortOrder;
}
