package com.xinyu.article.mapper;

import lombok.Data;

@Data
public class ArticleTagRow {
    private Long articleId;
    private Long id;
    private String name;
    private String slug;
}
