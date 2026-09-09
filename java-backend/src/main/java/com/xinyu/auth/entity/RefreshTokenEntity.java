package com.xinyu.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xinyu.common.mybatis.typehandler.PostgreSqlUuidTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName(value = "refresh_tokens", autoResultMap = true)
public class RefreshTokenEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField(value = "family_id", typeHandler = PostgreSqlUuidTypeHandler.class)
    private UUID familyId;
    @TableField("token_hash")
    private String tokenHash;
    @TableField("device_info")
    private String deviceInfo;
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    @TableField("used_at")
    private OffsetDateTime usedAt;
    @TableField("revoked_at")
    private OffsetDateTime revokedAt;
    @TableField("revocation_reason")
    private String revocationReason;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
