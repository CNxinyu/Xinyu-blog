package com.xinyu.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.auth.service.RefreshTokenRevocationService;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.dto.ProfileUpdateRequest;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.mapper.UserMapper;
import com.xinyu.user.model.UserRole;
import com.xinyu.user.model.UserStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final RefreshTokenRevocationService refreshTokenRevocationService;

    public UserService(UserMapper userMapper, RefreshTokenRevocationService refreshTokenRevocationService) {
        this.userMapper = userMapper;
        this.refreshTokenRevocationService = refreshTokenRevocationService;
    }

    @Transactional
    public UserEntity register(String username, String email, String passwordHash) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        if (userMapper.selectByIdentifier(normalizedUsername) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "username already exists",
                    java.util.Map.of("field", "username"));
        }
        if (userMapper.selectByIdentifier(normalizedEmail) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "email already exists",
                    java.util.Map.of("field", "email"));
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(normalizedUsername);
        entity.setEmail(normalizedEmail);
        entity.setPasswordHash(passwordHash);
        entity.setRole(UserRole.USER.name());
        entity.setStatus(UserStatus.ACTIVE.name());
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "username or email already exists");
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public UserEntity findByIdentifier(String identifier) {
        return userMapper.selectByIdentifier(normalize(identifier));
    }

    @Transactional(readOnly = true)
    public UserEntity requireById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public IPage<UserEntity> page(int page, int size, String keyword, UserStatus status, UserRole role) {
        LambdaQueryWrapper<UserEntity> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(UserEntity::getUsername, normalizedKeyword)
                    .or()
                    .like(UserEntity::getEmail, normalizedKeyword));
        }
        if (status != null) {
            query.eq(UserEntity::getStatus, status.name());
        }
        if (role != null) {
            query.eq(UserEntity::getRole, role.name());
        }
        query.orderByDesc(UserEntity::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional
    public UserEntity changeStatus(Long id, UserStatus status, Long operatorId) {
        UserEntity entity = requireById(id);
        if (status == UserStatus.DISABLED
                && UserStatus.ACTIVE.name().equals(entity.getStatus())
                && UserRole.ADMIN.name().equals(entity.getRole())
                && userMapper.lockActiveAdminIds().size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_PROTECTED);
        }

        OffsetDateTime updatedAt = now();
        if (userMapper.updateStatus(id, status.name(), updatedAt) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        if (status == UserStatus.DISABLED) {
            refreshTokenRevocationService.revokeAllByUserId(id, "USER_DISABLED");
        }
        entity.setStatus(status.name());
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    @Transactional
    public UserEntity changeRole(Long id, UserRole role, Long operatorId) {
        UserEntity entity = requireById(id);
        if (role == UserRole.USER
                && UserRole.ADMIN.name().equals(entity.getRole())
                && (id.equals(operatorId) || userMapper.lockActiveAdminIds().size() <= 1)) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_PROTECTED);
        }

        if (UserRole.valueOf(entity.getRole()) != role) {
            OffsetDateTime updatedAt = now();
            if (userMapper.updateRole(id, role.name(), updatedAt) != 1) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
            }
            refreshTokenRevocationService.revokeAllByUserId(id, "ROLE_CHANGED");
            entity.setRole(role.name());
            entity.setUpdatedAt(updatedAt);
        }
        return entity;
    }

    @Transactional
    public UserEntity updateProfile(Long id, ProfileUpdateRequest request) {
        if (!request.hasChanges()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "profile update requires at least one field");
        }

        requireById(id);
        int updated = userMapper.updateProfile(
                id,
                trimToNull(request.getNickname()),
                request.hasNickname(),
                trimToNull(request.getAvatarUrl()),
                request.hasAvatarUrl(),
                trimToNull(request.getBio()),
                request.hasBio(),
                now());
        if (updated != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return requireById(id);
    }

    @Transactional(readOnly = true)
    public UserEntity profile(Long id) {
        return requireById(id);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
