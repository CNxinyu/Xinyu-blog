package com.xinyu.entity;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // log.debug("MyBatis-Plus insert fill start ...");

        // 创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 更新时间（插入时也填充）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 逻辑删除标志默认 0
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);

        // 如果你有创建人、更新人字段，可以在这里获取当前登录用户ID
        // Long currentUserId = SecurityUtil.getCurrentUserId();
        // this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        // this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // log.debug("MyBatis-Plus update fill start ...");

        // 更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 更新人
        // Long currentUserId = SecurityUtil.getCurrentUserId();
        // this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }
}