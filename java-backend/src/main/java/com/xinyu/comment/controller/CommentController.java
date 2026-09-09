package com.xinyu.comment.controller;

import com.xinyu.comment.dto.CommentCreateRequest;
import com.xinyu.comment.dto.CommentResponse;
import com.xinyu.comment.dto.CommentStatusUpdateRequest;
import com.xinyu.comment.entity.CommentEntity;
import com.xinyu.comment.model.CommentStatus;
import com.xinyu.comment.service.CommentService;
import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@Tag(name = "Comment", description = "Article comments and moderation")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/articles/{articleId}/comments")
    @Operation(summary = "List approved comments for an article")
    public ApiResponse<PageResponse<CommentResponse>> publicPage(
            @PathVariable @Positive Long articleId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(PageResponse.from(
                commentService.publicPage(articleId, page, size), CommentResponse::from));
    }

    @PostMapping("/api/articles/{articleId}/comments")
    @Operation(summary = "Submit a comment for moderation")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CommentResponse> create(
            @PathVariable @Positive Long articleId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(CommentResponse.from(
                commentService.create(articleId, userId(jwt), request)));
    }

    @GetMapping("/api/admin/comments")
    @Operation(summary = "List comments for moderation")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<CommentResponse>> adminPage(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) @Positive Long articleId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(PageResponse.from(
                commentService.adminPage(page, size, status, articleId, keyword), CommentResponse::from));
    }

    @PutMapping("/api/admin/comments/{id}/status")
    @Operation(summary = "Approve or reject a comment")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CommentResponse> updateStatus(@PathVariable @Positive Long id,
                                                      @Valid @RequestBody CommentStatusUpdateRequest request) {
        return ApiResponse.success(CommentResponse.from(commentService.updateStatus(id, request.status())));
    }

    @DeleteMapping("/api/admin/comments/{id}")
    @Operation(summary = "Delete a comment and its replies")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        commentService.delete(id);
        return ApiResponse.success();
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
