package com.xinyu.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesFieldsAndPasswordConfirmation() {
        RegisterRequest request = new RegisterRequest("a", "not-an-email", "short", "different");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "email", "password", "confirmPassword");
    }

    @Test
    void acceptsValidRegistrationRequest() {
        RegisterRequest request = new RegisterRequest("alice_1", "alice@example.com", "password123",
                "password123");

        assertThat(validator.validate(request)).isEmpty();
    }
}
