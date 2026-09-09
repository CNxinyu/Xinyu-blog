package com.xinyu.comment.dto;

import com.xinyu.comment.entity.CommentEntity;
import com.xinyu.comment.model.CommentStatus;

import java.time.OffsetDateTime;

public record CommentResponse(
        Long id,
        Long articleId,
        Long parentId,
        String content,
        CommentStatus status,
        CommentAuthorResponse author,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommentResponse from(CommentEntity entity) {
        return new CommentResponse(
                entity.getId(), entity.getArticleId(), entity.getParentId(), entity.getContent(),
                CommentStatus.valueOf(entity.getStatus()),
                new CommentAuthorResponse(entity.getUserId(), entity.getAuthorUsername(),
                        entity.getAuthorNickname(), entity.getAuthorAvatarUrl()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
