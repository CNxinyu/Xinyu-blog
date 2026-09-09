package com.xinyu.auth.service;

import com.xinyu.common.security.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieService {
    private static final String COOKIE_PATH = "/api/v1/auth";
    private final JwtProperties properties;

    public RefreshCookieService(JwtProperties properties) {
        this.properties = properties;
    }

    public void write(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite(properties.getSameSite())
                .path(COOKIE_PATH)
                .maxAge(properties.getRefreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite(properties.getSameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
