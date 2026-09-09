package com.xinyu.article.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("article_tags")
public class ArticleTagEntity {
    @TableField("article_id")
    private Long articleId;

    @TableField("tag_id")
    private Long tagId;

    @TableField(value = "created_at")
    private OffsetDateTime createdAt;
}
