package com.xinyu.auth.service;

import com.xinyu.auth.dto.AuthResponse;
import com.xinyu.auth.dto.LoginRequest;
import com.xinyu.auth.dto.RegisterRequest;
import com.xinyu.auth.security.UserPrincipal;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    @Test
    void registerEncodesPasswordAndCreatesActiveUserThroughUserService() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123", "password123");
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}encoded");
        when(userService.register("alice", "alice@example.com", "{bcrypt}encoded")).thenReturn(user);

        AuthService service = service();

        assertThat(service.register(request).id()).isEqualTo(7L);
        verify(userService).register("alice", "alice@example.com", "{bcrypt}encoded");
    }

    @Test
    void loginReturnsAccessTokenAndRefreshToken() {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserPrincipal.from(user));
        when(jwtTokenService.issue(any())).thenReturn(new JwtTokenService.IssuedAccessToken("access", 900));
        when(refreshTokenService.issue(7L, "browser"))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh", user));

        AuthService.AuthResult result = service().login(
                new LoginRequest("alice", "password123"), "browser");

        assertThat(result.response()).isEqualTo(new AuthResponse("access", "Bearer", 900,
                com.xinyu.user.dto.UserResponse.from(user)));
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void invalidCredentialsAreMappedToGenericAuthenticationError() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("wrong password"));

        assertThatThrownBy(() -> service().login(new LoginRequest("alice", "wrong"), null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        verify(jwtTokenService, never()).issue(any());
        verify(refreshTokenService, never()).issue(any(), any());
    }

    @Test
    void refreshIssuesNewAccessTokenFromRotatedUser() {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("new-refresh", user));
        when(jwtTokenService.issue(any())).thenReturn(new JwtTokenService.IssuedAccessToken("new-access", 900));

        AuthService.AuthResult result = service().refresh("old-refresh");

        assertThat(result.response().accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logoutDelegatesToRefreshTokenRevocation() {
        service().logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    private AuthService service() {
        return new AuthService(userService, passwordEncoder, authenticationManager,
                jwtTokenService, refreshTokenService);
    }

    private UserEntity user(Long id, String role, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
