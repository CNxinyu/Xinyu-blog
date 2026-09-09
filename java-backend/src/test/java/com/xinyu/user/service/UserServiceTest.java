package com.xinyu.user.service;

import com.xinyu.auth.service.RefreshTokenRevocationService;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.dto.ProfileUpdateRequest;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.mapper.UserMapper;
import com.xinyu.user.model.UserRole;
import com.xinyu.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRevocationService refreshTokenRevocationService;

    @Test
    void profilePatchUpdatesOnlySuppliedFieldsAndAllowsNull() {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateProfile(eq(7L), eq("Alice"), eq(true), eq(null), eq(false),
                eq(null), eq(true), any())).thenReturn(1);

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname(" Alice ");
        request.setBio(null);

        UserEntity result = service().updateProfile(7L, request);

        assertThat(result).isSameAs(user);
        verify(userMapper).updateProfile(eq(7L), eq("Alice"), eq(true), eq(null), eq(false),
                eq(null), eq(true), any());
    }

    @Test
    void protectsLastActiveAdministratorFromRoleChange() {
        UserEntity admin = user(7L, "ADMIN", "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(admin);
        when(userMapper.lockActiveAdminIds()).thenReturn(List.of(7L));

        assertThatThrownBy(() -> service().changeRole(7L, UserRole.USER, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.LAST_ADMIN_PROTECTED);
        verify(userMapper, never()).updateRole(any(), any(), any());
    }

    @Test
    void roleChangeRevokesAllTargetSessions() {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateRole(eq(7L), eq("ADMIN"), any())).thenReturn(1);

        UserEntity result = service().changeRole(7L, UserRole.ADMIN, 9L);

        assertThat(result.getRole()).isEqualTo("ADMIN");
        verify(refreshTokenRevocationService).revokeAllByUserId(7L, "ROLE_CHANGED");
    }

    @Test
    void disablingUserRevokesAllSessions() {
        UserEntity user = user(7L, "USER", "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateStatus(eq(7L), eq("DISABLED"), any())).thenReturn(1);

        UserEntity result = service().changeStatus(7L, UserStatus.DISABLED, 9L);

        assertThat(result.getStatus()).isEqualTo("DISABLED");
        verify(refreshTokenRevocationService).revokeAllByUserId(7L, "USER_DISABLED");
    }

    private UserService service() {
        return new UserService(userMapper, refreshTokenRevocationService);
    }

    private UserEntity user(Long id, String role, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("user_1");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
