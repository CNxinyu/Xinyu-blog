package com.xinyu.article.controller;

import com.xinyu.article.dto.ArticleSummaryResponse;
import com.xinyu.article.service.ArticleService;
import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/search")
@Tag(name = "Article", description = "PostgreSQL article search")
public class SearchController {
    private final ArticleService articleService;

    public SearchController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "Search published articles by title, summary, category or tag")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> search(
            @RequestParam("q") @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(PageResponse.from(
                articleService.publicPage(page, size, query, null, null), ArticleSummaryResponse::from));
    }
}
