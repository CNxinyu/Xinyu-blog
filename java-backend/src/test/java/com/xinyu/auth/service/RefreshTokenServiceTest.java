package com.xinyu.auth.service;

import com.xinyu.auth.entity.RefreshTokenEntity;
import com.xinyu.auth.mapper.RefreshTokenMapper;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.common.security.JwtProperties;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void issueStoresOnlyHashAndReturnsOpaqueToken() {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        RefreshTokenService service = new RefreshTokenService(refreshTokenMapper, userService, properties);
        UserEntity user = activeUser(7L);
        when(userService.requireById(7L)).thenReturn(user);

        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "browser");

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertThat(issued.value()).isNotBlank().doesNotContain(".");
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(issued.value());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    void reuseRevokesWholeFamily() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(9L);
        entity.setFamilyId(UUID.randomUUID());
        entity.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(refreshTokenMapper.selectForUpdate(anyString())).thenReturn(entity);

        JwtProperties properties = new JwtProperties();
        RefreshTokenService service = new RefreshTokenService(refreshTokenMapper, userService, properties);

        assertThatThrownBy(() -> service.rotate("old-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_REUSED);
        verify(refreshTokenMapper).revokeFamily(any(UUID.class), any(OffsetDateTime.class),
                org.mockito.ArgumentMatchers.eq("REUSE_DETECTED"));
    }

    @Test
    void rotateMarksPreviousTokenAndIssuesNewTokenInSameFamily() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(9L);
        entity.setUserId(7L);
        entity.setFamilyId(UUID.randomUUID());
        entity.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        entity.setDeviceInfo("browser");
        when(refreshTokenMapper.selectForUpdate(anyString())).thenReturn(entity);
        when(refreshTokenMapper.markUsed(any(Long.class), any(OffsetDateTime.class), any(OffsetDateTime.class),
                anyString())).thenReturn(1);
        UserEntity user = activeUser(7L);
        when(userService.requireById(7L)).thenReturn(user);

        JwtProperties properties = new JwtProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        RefreshTokenService service = new RefreshTokenService(refreshTokenMapper, userService, properties);

        RefreshTokenService.IssuedRefreshToken rotated = service.rotate("old-token");

        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.value()).isNotBlank();
        verify(refreshTokenMapper).markUsed(any(Long.class), any(OffsetDateTime.class),
                any(OffsetDateTime.class), org.mockito.ArgumentMatchers.eq("ROTATED"));
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo(entity.getFamilyId());
    }

    private UserEntity activeUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("user_1");
        user.setStatus("ACTIVE");
        user.setRole("USER");
        return user;
    }
}
