package com.xinyu.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinyu.user.entity.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

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
}
