package com.xinyu.article.dto;

import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.model.ArticleStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ArticleDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        String contentHtml,
        ArticleStatus status,
        CategoryResponse category,
        List<TagResponse> tags,
        ArticleAuthorResponse author,
        OffsetDateTime publishedAt,
        OffsetDateTime archivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ArticleDetailResponse from(ArticleEntity entity) {
        return new ArticleDetailResponse(
                entity.getId(), entity.getTitle(), entity.getSlug(), entity.getSummary(),
                entity.getContentMarkdown(), entity.getContentHtml(), ArticleStatus.valueOf(entity.getStatus()),
                ArticleSummaryResponse.category(entity), ArticleSummaryResponse.tags(entity),
                ArticleSummaryResponse.author(entity), entity.getPublishedAt(), entity.getArchivedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
