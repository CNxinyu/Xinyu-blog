package com.xinyu.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    @Test
    void delegatesToBcryptAndMatchesPassword() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        String encoded = encoder.encode("correct horse battery staple");

        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(encoder.matches("correct horse battery staple", encoded)).isTrue();
        assertThat(encoder.matches("wrong password", encoded)).isFalse();
    }
}
