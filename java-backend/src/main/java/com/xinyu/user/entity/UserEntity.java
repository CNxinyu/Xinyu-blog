package com.xinyu.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("users")
public class UserEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String email;
    @TableField("password_hash")
    private String passwordHash;
    private String nickname;
    @TableField("avatar_url")
    private String avatarUrl;
    private String bio;
    private String role;
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
