package com.xinyu.auth.dto;

import com.xinyu.user.dto.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
