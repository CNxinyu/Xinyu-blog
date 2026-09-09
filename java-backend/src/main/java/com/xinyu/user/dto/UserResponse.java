package com.xinyu.user.dto;

import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.model.UserRole;
import com.xinyu.user.model.UserStatus;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String bio,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserResponse from(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getNickname(),
                entity.getAvatarUrl(),
                entity.getBio(),
                UserRole.valueOf(entity.getRole()),
                UserStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
