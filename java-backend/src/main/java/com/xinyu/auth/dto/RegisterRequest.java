package com.xinyu.auth.dto;

import com.xinyu.common.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordMatches
public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 30, message = "username must be between 3 and 30 characters")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "username may contain letters, numbers and underscores")
        String username,
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        @Size(max = 254, message = "email is too long")
        String email,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password,
        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
