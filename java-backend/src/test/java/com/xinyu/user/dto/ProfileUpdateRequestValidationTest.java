package com.xinyu.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUpdateRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void supportsPartialUpdatesAndExplicitClears() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("Alice");
        request.setBio(null);

        assertThat(request.hasChanges()).isTrue();
        assertThat(request.hasNickname()).isTrue();
        assertThat(request.hasBio()).isTrue();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankNicknameAndOversizedBio() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("   ");
        request.setBio("x".repeat(1001));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("nickname", "bio");
    }
}
