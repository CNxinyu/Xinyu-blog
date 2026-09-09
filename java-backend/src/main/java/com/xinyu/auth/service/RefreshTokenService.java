package com.xinyu.auth.service;

import com.xinyu.auth.entity.RefreshTokenEntity;
import com.xinyu.auth.mapper.RefreshTokenMapper;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.common.security.JwtProperties;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserService userService;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper, UserService userService,
                               JwtProperties properties) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.userService = userService;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issue(Long userId, String deviceInfo) {
        return issue(userId, UUID.randomUUID(), deviceInfo);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        RefreshTokenEntity current = refreshTokenMapper.selectForUpdate(hash(rawToken));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (current == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        if (current.getUsedAt() != null || current.getRevokedAt() != null) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_REUSED);
        }
        if (current.getExpiresAt() == null || current.getExpiresAt().isBefore(now)) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "EXPIRED");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        UserEntity user = userService.requireById(current.getUserId());
        if (!"ACTIVE".equals(user.getStatus())) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "USER_DISABLED");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        int updated = refreshTokenMapper.markUsed(current.getId(), now, now, "ROTATED");
        if (updated != 1) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_REUSED);
        }
        return issue(user.getId(), current.getFamilyId(), current.getDeviceInfo(), user);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshTokenEntity current = refreshTokenMapper.selectForUpdate(hash(rawToken));
        if (current != null && current.getRevokedAt() == null) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), OffsetDateTime.now(ZoneOffset.UTC), "LOGOUT");
        }
    }

    private IssuedRefreshToken issue(Long userId, UUID familyId, String deviceInfo) {
        return issue(userId, familyId, deviceInfo, userService.requireById(userId));
    }

    private IssuedRefreshToken issue(Long userId, UUID familyId, String deviceInfo, UserEntity user) {
        String rawToken = randomToken();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setTokenHash(hash(rawToken));
        entity.setDeviceInfo(limit(deviceInfo, 255));
        entity.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plus(properties.getRefreshTokenTtl()));
        refreshTokenMapper.insert(entity);
        return new IssuedRefreshToken(rawToken, user);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        SecureRandomHolder.INSTANCE.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record IssuedRefreshToken(String value, UserEntity user) {
    }

    private static final class SecureRandomHolder {
        private static final java.security.SecureRandom INSTANCE = new java.security.SecureRandom();
    }
}
