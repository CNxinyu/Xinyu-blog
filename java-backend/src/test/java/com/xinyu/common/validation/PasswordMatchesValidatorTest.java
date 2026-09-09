package com.xinyu.common.validation;

import com.xinyu.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordMatchesValidatorTest {

    private final PasswordMatchesValidator validator = new PasswordMatchesValidator();

    @Test
    void acceptsMatchingPasswords() {
        RegisterRequest request = new RegisterRequest("user_1", "user@example.com", "password123",
                "password123");

        assertThat(validator.isValid(request, null)).isTrue();
    }

    @Test
    void rejectsDifferentPasswords() {
        RegisterRequest request = new RegisterRequest("user_1", "user@example.com", "password123",
                "password456");

        assertThat(validator.isValid(request, null)).isFalse();
    }
}
