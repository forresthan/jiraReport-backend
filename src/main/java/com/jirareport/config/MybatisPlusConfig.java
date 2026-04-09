package com.jirareport.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                // 不用 strictInsertFill：子类继承 BaseEntity 时 getGetterType(createdAt) 可能为 null，导致跳过填充
                if (metaObject.hasSetter("createdAt") && metaObject.getValue("createdAt") == null) {
                    metaObject.setValue("createdAt", now);
                }
                if (metaObject.hasSetter("updatedAt") && metaObject.getValue("updatedAt") == null) {
                    metaObject.setValue("updatedAt", now);
                }
                if (metaObject.hasSetter("deleted") && metaObject.getValue("deleted") == null) {
                    metaObject.setValue("deleted", 0);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                if (metaObject.hasSetter("updatedAt")) {
                    metaObject.setValue("updatedAt", LocalDateTime.now());
                }
            }
        };
    }
}