package com.xinyu.article.dto;

import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.model.ArticleStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ArticleSummaryResponse(
        Long id,
        String title,
        String slug,
        String summary,
        ArticleStatus status,
        CategoryResponse category,
        List<TagResponse> tags,
        ArticleAuthorResponse author,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ArticleSummaryResponse from(ArticleEntity entity) {
        return new ArticleSummaryResponse(
                entity.getId(), entity.getTitle(), entity.getSlug(), entity.getSummary(),
                ArticleStatus.valueOf(entity.getStatus()), category(entity), tags(entity), author(entity),
                entity.getPublishedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static CategoryResponse category(ArticleEntity entity) {
        return entity.getCategoryId() == null ? null
                : new CategoryResponse(entity.getCategoryId(), entity.getCategoryName(), entity.getCategorySlug(),
                entity.getCategoryDescription(), null, null);
    }

    static List<TagResponse> tags(ArticleEntity entity) {
        return entity.getTags() == null ? List.of() : entity.getTags().stream().map(TagResponse::from).toList();
    }

    static ArticleAuthorResponse author(ArticleEntity entity) {
        return entity.getAuthorId() == null ? null
                : new ArticleAuthorResponse(entity.getAuthorId(), entity.getAuthorUsername(),
                entity.getAuthorNickname(), entity.getAuthorAvatarUrl());
    }
}
