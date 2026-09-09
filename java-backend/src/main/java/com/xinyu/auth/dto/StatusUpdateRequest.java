package com.xinyu.auth.dto;

import com.xinyu.user.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull(message = "status is required") UserStatus status) {
}
