package com.xinyu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "verify-code")
public class VerifyCodeProperties {
    private int ttlSeconds = 300;
    private int cooldownSeconds = 60;
    private int maxAttempts = 5;
    private int lockSeconds = 600;
    private String hmacSecret = "HUOUWJGYIECRSNBH";
    private String mailFrom = "18156944123@163.com";

}
