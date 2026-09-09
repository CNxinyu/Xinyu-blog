package com.xinyu.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.mapper.UserMapper;
import com.xinyu.user.model.UserRole;
import com.xinyu.user.model.UserStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
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
    public IPage<UserEntity> page(int page, int size, String keyword, UserStatus status) {
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
        query.orderByDesc(UserEntity::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional
    public UserEntity changeStatus(Long id, UserStatus status) {
        UserEntity entity = requireById(id);
        entity.setStatus(status.name());
        userMapper.updateById(entity);
        return entity;
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
