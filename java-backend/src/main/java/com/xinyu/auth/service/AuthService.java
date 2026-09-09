package com.xinyu.auth.service;

import com.xinyu.auth.dto.AuthResponse;
import com.xinyu.auth.dto.LoginRequest;
import com.xinyu.auth.dto.RegisterRequest;
import com.xinyu.auth.security.UserPrincipal;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.dto.UserResponse;
import com.xinyu.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenService jwtTokenService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        UserEntity user = userService.register(request.username(), request.email(),
                passwordEncoder.encode(request.password()));
        return UserResponse.from(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String deviceInfo) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.identifier(), request.password()));
        } catch (DisabledException exception) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException exception) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(principal);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(principal.getUserId(), deviceInfo);
        return new AuthResult(new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresIn(),
                UserResponse.from(refreshToken.user())), refreshToken.value());
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(UserPrincipal.from(rotated.user()));
        return new AuthResult(new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresIn(),
                UserResponse.from(rotated.user())), rotated.value());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return UserResponse.from(userService.requireById(userId));
    }

    public record AuthResult(AuthResponse response, String refreshToken) {
    }
}
