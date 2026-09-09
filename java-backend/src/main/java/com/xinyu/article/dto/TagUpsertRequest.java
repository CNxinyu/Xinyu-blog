package com.xinyu.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TagUpsertRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must use lowercase letters, numbers and hyphens")
        String slug
) {
}
