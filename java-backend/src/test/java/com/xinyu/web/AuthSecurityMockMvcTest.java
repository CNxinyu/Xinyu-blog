package com.xinyu.web;

import com.xinyu.auth.service.AuthService;
import com.xinyu.auth.service.RefreshCookieService;
import com.xinyu.common.security.AuthRateLimitService;
import com.xinyu.common.security.JwtProperties;
import com.xinyu.common.security.SecurityConfig;
import com.xinyu.common.web.TraceIdFilter;
import com.xinyu.auth.controller.AuthController;
import com.xinyu.user.controller.AdminUserController;
import com.xinyu.user.controller.UserController;
import com.xinyu.user.dto.ProfileUpdateRequest;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@WebMvcTest(controllers = {AuthController.class, UserController.class, AdminUserController.class})
@Import({SecurityConfig.class, TraceIdFilter.class})
class AuthSecurityMockMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshCookieService refreshCookieService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private AuthRateLimitService authRateLimitService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithCsrfReturnsAccessTokenAndWritesRefreshCookie() throws Exception {
        when(authService.login(any(), any()))
                .thenReturn(new AuthService.AuthResult(
                        new com.xinyu.auth.dto.AuthResponse("access-token", "Bearer", 900, null),
                        "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));

        verify(refreshCookieService).write(any(), anyString());
    }

    @Test
    void rateLimitReturnsTooManyRequestsAndRetryAfter() throws Exception {
        doThrow(new com.xinyu.common.exception.BusinessException(
                com.xinyu.common.api.ErrorCode.AUTH_RATE_LIMITED,
                "too many authentication attempts",
                Map.of("retryAfterSeconds", 60)))
                .when(authRateLimitService).checkLogin(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getHeader("Retry-After")).isEqualTo("60"));
    }

    @Test
    void refreshUsesCookieAndWritesRotatedCookie() throws Exception {
        when(jwtProperties.getRefreshCookieName()).thenReturn("refresh_token");
        when(authService.refresh("old-refresh-token"))
                .thenReturn(new AuthService.AuthResult(
                        new com.xinyu.auth.dto.AuthResponse("new-access-token", "Bearer", 900, null),
                        "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

        verify(refreshCookieService).write(any(), eq("new-refresh-token"));
    }

    @Test
    void logoutClearsCurrentRefreshCookie() throws Exception {
        when(jwtProperties.getRefreshCookieName()).thenReturn("refresh_token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk());

        verify(authService).logout("refresh-token");
        verify(refreshCookieService).clear(any());
    }

    @Test
    void profileRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadProfile() throws Exception {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(userService.profile(7L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void authenticatedUserCanPatchProfile() throws Exception {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(userService.updateProfile(eq(7L), any(ProfileUpdateRequest.class))).thenReturn(user);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/users/me")
                        .with(jwt().jwt(token -> token.subject("7")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Alice\",\"bio\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminUsers() throws Exception {
        IPage<com.xinyu.user.entity.UserEntity> page = new Page<>(1, 20);
        when(userService.page(anyInt(), anyInt(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private UserEntity user(Long id, String role, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("user_1");
        user.setEmail("user@example.com");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
