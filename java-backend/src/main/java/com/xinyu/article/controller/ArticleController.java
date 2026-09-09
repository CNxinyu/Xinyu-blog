package com.xinyu.article.controller;

import com.xinyu.article.dto.ArticleDetailResponse;
import com.xinyu.article.dto.ArticleSummaryResponse;
import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.service.ArticleService;
import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/articles")
@Tag(name = "Article", description = "Published article browsing")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "List published articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long tagId) {
        return ApiResponse.success(PageResponse.from(
                articleService.publicPage(page, size, keyword, categoryId, tagId),
                ArticleSummaryResponse::from));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a published article by slug")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable String slug) {
        ArticleEntity article = articleService.requirePublishedBySlug(slug);
        return ApiResponse.success(ArticleDetailResponse.from(article));
    }
}
