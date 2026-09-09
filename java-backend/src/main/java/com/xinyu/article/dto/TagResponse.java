package com.xinyu.article.dto;

import com.xinyu.article.entity.TagEntity;

import java.time.OffsetDateTime;

public record TagResponse(Long id, String name, String slug,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static TagResponse from(TagEntity entity) {
        return new TagResponse(entity.getId(), entity.getName(), entity.getSlug(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
