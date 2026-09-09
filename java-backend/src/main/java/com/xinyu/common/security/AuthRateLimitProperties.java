package com.xinyu.common.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "xinyu.security.rate-limit")
public class AuthRateLimitProperties {
    @Min(1)
    private int registerMaxAttempts = 10;
    private Duration registerWindow = Duration.ofHours(1);
    @Min(1)
    private int loginIpMaxAttempts = 30;
    private Duration loginIpWindow = Duration.ofMinutes(10);
    @Min(1)
    private int loginIdentifierFailureMaxAttempts = 5;
    private Duration loginIdentifierFailureWindow = Duration.ofMinutes(15);

    public int getRegisterMaxAttempts() {
        return registerMaxAttempts;
    }

    public void setRegisterMaxAttempts(int registerMaxAttempts) {
        this.registerMaxAttempts = registerMaxAttempts;
    }

    public Duration getRegisterWindow() {
        return registerWindow;
    }

    public void setRegisterWindow(Duration registerWindow) {
        this.registerWindow = registerWindow;
    }

    public int getLoginIpMaxAttempts() {
        return loginIpMaxAttempts;
    }

    public void setLoginIpMaxAttempts(int loginIpMaxAttempts) {
        this.loginIpMaxAttempts = loginIpMaxAttempts;
    }

    public Duration getLoginIpWindow() {
        return loginIpWindow;
    }

    public void setLoginIpWindow(Duration loginIpWindow) {
        this.loginIpWindow = loginIpWindow;
    }

    public int getLoginIdentifierFailureMaxAttempts() {
        return loginIdentifierFailureMaxAttempts;
    }

    public void setLoginIdentifierFailureMaxAttempts(int loginIdentifierFailureMaxAttempts) {
        this.loginIdentifierFailureMaxAttempts = loginIdentifierFailureMaxAttempts;
    }

    public Duration getLoginIdentifierFailureWindow() {
        return loginIdentifierFailureWindow;
    }

    public void setLoginIdentifierFailureWindow(Duration loginIdentifierFailureWindow) {
        this.loginIdentifierFailureWindow = loginIdentifierFailureWindow;
    }
}
