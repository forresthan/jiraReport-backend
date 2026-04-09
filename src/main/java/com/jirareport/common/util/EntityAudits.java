package com.jirareport.common.util;

import com.jirareport.mapper.entity.BaseEntity;

import java.time.LocalDateTime;

public final class EntityAudits {

    private EntityAudits() {}

    /** 插入前写入审计字段；防止部分继承实体上 strictInsertFill 未写入库的情况 */
    public static void stampForInsert(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getUpdatedAt() == null) {
            entity.setUpdatedAt(now);
        }
        if (entity.getDeleted() == null) {
            entity.setDeleted(0);
        }
    }
}
