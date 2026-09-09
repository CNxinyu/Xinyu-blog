package com.xinyu.auth.service;

import com.xinyu.auth.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class RefreshTokenRevocationService {
    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenRevocationService(RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenMapper = refreshTokenMapper;
    }

    public int revokeAllByUserId(Long userId, String reason) {
        return refreshTokenMapper.revokeAllByUserId(userId,
                OffsetDateTime.now(ZoneOffset.UTC), reason);
    }
}
