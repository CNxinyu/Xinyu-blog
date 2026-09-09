package com.xinyu.auth.service;

import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.common.security.AuthRateLimitProperties;
import com.xinyu.common.security.AuthRateLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void rejectsRegisterWhenIpWindowIsExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(11L);

        AuthRateLimitService service = new AuthRateLimitService(redisTemplate, new AuthRateLimitProperties());

        assertThatThrownBy(() -> service.checkRegister("127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_RATE_LIMITED);
    }

    @Test
    void rejectsLoginWhenIdentifierFailureWindowIsExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5");

        AuthRateLimitService service = new AuthRateLimitService(redisTemplate, new AuthRateLimitProperties());

        assertThatThrownBy(() -> service.checkLogin("127.0.0.1", "Alice@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_RATE_LIMITED);
    }

    @Test
    void successfulLoginClearsIdentifierFailureCounter() {
        AuthRateLimitService service = new AuthRateLimitService(redisTemplate, new AuthRateLimitProperties());

        service.recordLoginSuccess("127.0.0.1", "Alice@example.com");

        verify(redisTemplate).delete(anyString());
    }
}
