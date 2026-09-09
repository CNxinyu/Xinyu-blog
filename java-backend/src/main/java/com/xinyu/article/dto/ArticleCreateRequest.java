package com.xinyu.article.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 255)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must use lowercase letters, numbers and hyphens")
        String slug,
        @Size(max = 500) String summary,
        @NotNull @Positive Long categoryId,
        @Size(max = 30) List<@Valid @Positive Long> tagIds,
        @Size(max = 1_000_000) String contentMarkdown
) {
}
