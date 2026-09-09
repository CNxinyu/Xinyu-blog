package com.xinyu.comment.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank @Size(max = 2000) String content,
        @Positive Long parentId
) {
}
