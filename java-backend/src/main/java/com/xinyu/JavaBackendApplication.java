package com.xinyu;

import com.xinyu.config.VerifyCodeProperties;
import com.xinyu.common.security.AuthRateLimitProperties;
import com.xinyu.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({VerifyCodeProperties.class, JwtProperties.class, AuthRateLimitProperties.class})
public class JavaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaBackendApplication.class, args);
	}

}
