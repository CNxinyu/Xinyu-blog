package com.xinyu.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinyu.auth.entity.RefreshTokenEntity;
import com.xinyu.common.mybatis.typehandler.PostgreSqlUuidTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RefreshTokenMapper extends BaseMapper<RefreshTokenEntity> {
    @Results(id = "refreshTokenResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "familyId", column = "family_id",
                    typeHandler = PostgreSqlUuidTypeHandler.class),
            @Result(property = "tokenHash", column = "token_hash"),
            @Result(property = "deviceInfo", column = "device_info"),
            @Result(property = "expiresAt", column = "expires_at"),
            @Result(property = "usedAt", column = "used_at"),
            @Result(property = "revokedAt", column = "revoked_at"),
            @Result(property = "revocationReason", column = "revocation_reason"),
            @Result(property = "createdAt", column = "created_at")
    })
    @Select("""
            SELECT id, user_id, family_id, token_hash, device_info, expires_at,
                   used_at, revoked_at, revocation_reason, created_at
            FROM refresh_tokens
            WHERE token_hash = #{tokenHash}
            FOR UPDATE
            """)
    RefreshTokenEntity selectForUpdate(@Param("tokenHash") String tokenHash);

    @Update("""
            UPDATE refresh_tokens
            SET used_at = #{usedAt}, revoked_at = #{revokedAt}, revocation_reason = #{reason}
            WHERE id = #{id} AND used_at IS NULL AND revoked_at IS NULL
            """)
    int markUsed(@Param("id") Long id,
                 @Param("usedAt") OffsetDateTime usedAt,
                 @Param("revokedAt") OffsetDateTime revokedAt,
                 @Param("reason") String reason);

    @Update("""
            UPDATE refresh_tokens
            SET revoked_at = #{revokedAt}, revocation_reason = #{reason}
            WHERE family_id = #{familyId,jdbcType=OTHER,typeHandler=com.xinyu.common.mybatis.typehandler.PostgreSqlUuidTypeHandler}
              AND revoked_at IS NULL
            """)
    int revokeFamily(@Param("familyId") UUID familyId,
                     @Param("revokedAt") OffsetDateTime revokedAt,
                     @Param("reason") String reason);
}
