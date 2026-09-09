package com.xinyu.article.dto;

import com.xinyu.article.entity.CategoryEntity;

import java.time.OffsetDateTime;

public record CategoryResponse(Long id, String name, String slug, String description,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static CategoryResponse from(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
