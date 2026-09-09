package com.xinyu.user.controller;

import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.api.PageResponse;
import com.xinyu.user.dto.RoleUpdateRequest;
import com.xinyu.user.dto.UserResponse;
import com.xinyu.user.model.UserRole;
import com.xinyu.user.model.UserStatus;
import com.xinyu.user.service.UserService;
import com.xinyu.auth.dto.StatusUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "User")
@SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many authentication attempts"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Resource already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
})
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role) {
        return ApiResponse.success(PageResponse.from(
                userService.page(page, size, keyword, status, role), UserResponse::from));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> changeStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(UserResponse.from(
                userService.changeStatus(id, request.status(), userId(jwt))));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<UserResponse> changeRole(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(UserResponse.from(
                userService.changeRole(id, request.role(), userId(jwt))));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
