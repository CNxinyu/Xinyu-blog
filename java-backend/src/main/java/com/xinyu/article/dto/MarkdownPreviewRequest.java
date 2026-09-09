package com.xinyu.article.dto;

import jakarta.validation.constraints.Size;

public record MarkdownPreviewRequest(
        @Size(max = 1_000_000) String contentMarkdown
) {
}
