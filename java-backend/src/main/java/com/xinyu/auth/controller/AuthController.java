package com.xinyu.auth.controller;

import com.xinyu.auth.dto.AuthResponse;
import com.xinyu.auth.dto.LoginRequest;
import com.xinyu.auth.dto.RegisterRequest;
import com.xinyu.auth.service.AuthService;
import com.xinyu.auth.service.RefreshCookieService;
import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.security.JwtProperties;
import com.xinyu.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Resource not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Resource already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
})
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, RefreshCookieService refreshCookieService,
                           JwtProperties jwtProperties) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
        this.jwtProperties = jwtProperties;
    }

    @RequestMapping(value = "/csrf", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<String> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest,
                                           HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.login(request, httpRequest.getHeader("User-Agent"));
        refreshCookieService.write(httpResponse, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.refresh(refreshToken(httpRequest));
        refreshCookieService.write(httpResponse, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(refreshToken(httpRequest));
        refreshCookieService.clear(httpResponse);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(authService.currentUser(Long.valueOf(jwt.getSubject())));
    }

    private String refreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
