package com.xinyu.article.controller;

import com.xinyu.article.dto.ArticleCreateRequest;
import com.xinyu.article.dto.ArticleDetailResponse;
import com.xinyu.article.dto.ArticleUpdateRequest;
import com.xinyu.article.dto.MarkdownPreviewRequest;
import com.xinyu.article.dto.MarkdownPreviewResponse;
import com.xinyu.article.dto.ArticleSummaryResponse;
import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.model.ArticleStatus;
import com.xinyu.article.service.ArticleService;
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
@RequestMapping("/api/admin/articles")
@Tag(name = "Article", description = "Article authoring and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class AdminArticleController {
    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "List articles for administration")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long tagId) {
        return ApiResponse.success(PageResponse.from(
                articleService.adminPage(page, size, status, keyword, categoryId, tagId),
                ArticleSummaryResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an article for editing")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(ArticleDetailResponse.from(articleService.requireAdmin(id)));
    }

    @PostMapping
    @Operation(summary = "Create an article draft")
    public ApiResponse<ArticleDetailResponse> create(@Valid @RequestBody ArticleCreateRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(ArticleDetailResponse.from(
                articleService.create(request, userId(jwt))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update article content and taxonomy")
    public ApiResponse<ArticleDetailResponse> update(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody ArticleUpdateRequest request) {
        return ApiResponse.success(ArticleDetailResponse.from(articleService.update(id, request)));
    }

    @PostMapping("/{id}/draft")
    @Operation(summary = "Save the current article as a draft")
    public ApiResponse<ArticleDetailResponse> draft(@PathVariable @Positive Long id) {
        return ApiResponse.success(ArticleDetailResponse.from(articleService.saveDraft(id)));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish an article")
    public ApiResponse<ArticleDetailResponse> publish(@PathVariable @Positive Long id) {
        return ApiResponse.success(ArticleDetailResponse.from(articleService.publish(id)));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an article")
    public ApiResponse<ArticleDetailResponse> archive(@PathVariable @Positive Long id) {
        return ApiResponse.success(ArticleDetailResponse.from(articleService.archive(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an article")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        articleService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/preview")
    @Operation(summary = "Render Markdown without persisting it")
    public ApiResponse<MarkdownPreviewResponse> preview(@Valid @RequestBody MarkdownPreviewRequest request) {
        return ApiResponse.success(articleService.preview(request.contentMarkdown()));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
