package com.xinyu.auth.service;

import com.xinyu.common.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieServiceTest {
    @Test
    void writesHttpOnlyScopedSameSiteCookie() {
        JwtProperties properties = properties();
        RefreshCookieService service = new RefreshCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.write(response, "opaque-refresh-token");

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("refresh_token=opaque-refresh-token")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=2592000")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void clearsRefreshCookie() {
        RefreshCookieService service = new RefreshCookieService(properties());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        properties.setSecureCookie(false);
        properties.setSameSite("Lax");
        return properties;
    }
}
