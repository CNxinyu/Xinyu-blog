package com.xinyu.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinyu.user.entity.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

public interface UserMapper extends BaseMapper<UserEntity> {
    @Results(id = "userResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "username", column = "username"),
            @Result(property = "email", column = "email"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "nickname", column = "nickname"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "bio", column = "bio"),
            @Result(property = "role", column = "role"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("""
            SELECT id, username, email, password_hash, nickname, avatar_url, bio,
                   role, status, created_at, updated_at
            FROM users
            WHERE username = #{identifier} OR email = #{identifier}
            """)
    UserEntity selectByIdentifier(@Param("identifier") String identifier);

    @Select("SELECT id FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE' ORDER BY id FOR UPDATE")
    List<Long> lockActiveAdminIds();

    @Update("""
            UPDATE users
            SET status = #{status}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("updatedAt") OffsetDateTime updatedAt);

    @Update("""
            UPDATE users
            SET role = #{role}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateRole(@Param("id") Long id,
                   @Param("role") String role,
                   @Param("updatedAt") OffsetDateTime updatedAt);

    @Update("""
            <script>
            UPDATE users
            <set>
                <if test="nicknamePresent">nickname = #{nickname,jdbcType=VARCHAR},</if>
                <if test="avatarUrlPresent">avatar_url = #{avatarUrl,jdbcType=VARCHAR},</if>
                <if test="bioPresent">bio = #{bio,jdbcType=VARCHAR},</if>
                updated_at = #{updatedAt}
            </set>
            WHERE id = #{id}
            </script>
            """)
    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("nicknamePresent") boolean nicknamePresent,
                      @Param("avatarUrl") String avatarUrl,
                      @Param("avatarUrlPresent") boolean avatarUrlPresent,
                      @Param("bio") String bio,
                      @Param("bioPresent") boolean bioPresent,
                      @Param("updatedAt") OffsetDateTime updatedAt);
}
