package com.xinyu.comment.dto;

import com.xinyu.comment.model.CommentStatus;
import jakarta.validation.constraints.NotNull;

public record CommentStatusUpdateRequest(@NotNull CommentStatus status) {
}
