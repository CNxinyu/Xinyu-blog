package com.xinyu.common.security;

import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AuthRateLimitService {
    private static final String KEY_PREFIX = "xinyu:auth:rate-limit:";
    private static final RedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthRateLimitProperties properties;

    public AuthRateLimitService(StringRedisTemplate redisTemplate, AuthRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void checkRegister(String clientIp) {
        executeSafely(() -> {
            long attempts = increment(key("register:ip", clientIp), properties.getRegisterWindow());
            if (attempts > properties.getRegisterMaxAttempts()) {
                throw limited(properties.getRegisterWindow());
            }
        });
    }

    public void checkLogin(String clientIp, String identifier) {
        executeSafely(() -> {
            long attempts = increment(key("login:ip", clientIp), properties.getLoginIpWindow());
            if (attempts > properties.getLoginIpMaxAttempts()) {
                throw limited(properties.getLoginIpWindow());
            }

            String failureKey = loginFailureKey(clientIp, identifier);
            String failures = redisTemplate.opsForValue().get(failureKey);
            if (failures != null && Long.parseLong(failures) >= properties.getLoginIdentifierFailureMaxAttempts()) {
                throw limited(properties.getLoginIdentifierFailureWindow());
            }
        });
    }

    public void recordLoginFailure(String clientIp, String identifier) {
        executeSafely(() -> increment(loginFailureKey(clientIp, identifier),
                properties.getLoginIdentifierFailureWindow()));
    }

    public void recordLoginSuccess(String clientIp, String identifier) {
        executeSafely(() -> redisTemplate.delete(loginFailureKey(clientIp, identifier)));
    }

    private long increment(String key, Duration window) {
        Long value = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key),
                String.valueOf(window.toMillis()));
        if (value == null) {
            throw new IllegalStateException("rate limiter returned no counter");
        }
        return value;
    }

    private BusinessException limited(Duration retryAfter) {
        long seconds = Math.max(1, retryAfter.toSeconds());
        return new BusinessException(ErrorCode.AUTH_RATE_LIMITED,
                ErrorCode.AUTH_RATE_LIMITED.getMessage(),
                Map.of("retryAfterSeconds", seconds));
    }

    private void executeSafely(Runnable action) {
        try {
            action.run();
        } catch (BusinessException exception) {
            throw exception;
        } catch (DataAccessException | IllegalStateException | NumberFormatException exception) {
            log.warn("Auth rate limiter unavailable; allowing request");
        }
    }

    private String loginFailureKey(String clientIp, String identifier) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        return key("login:failure", clientIp + "\u0000" + normalizedIdentifier);
    }

    private String key(String category, String value) {
        return KEY_PREFIX + category + ":" + sha256(value == null ? "" : value);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
