package com.xinyu.user.dto;

import com.xinyu.user.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "role is required") UserRole role) {
}
