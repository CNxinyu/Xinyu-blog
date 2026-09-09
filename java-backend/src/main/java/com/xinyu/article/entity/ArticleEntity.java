package com.xinyu.article.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName("articles")
public class ArticleEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("author_id")
    private Long authorId;

    @TableField("category_id")
    private Long categoryId;

    private String title;
    private String slug;
    private String summary;

    @TableField("content_markdown")
    private String contentMarkdown;

    @TableField("content_html")
    private String contentHtml;

    private String status;

    @TableField("published_at")
    private OffsetDateTime publishedAt;

    @TableField("archived_at")
    private OffsetDateTime archivedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categorySlug;

    @TableField(exist = false)
    private String categoryDescription;

    @TableField(exist = false)
    private String authorUsername;

    @TableField(exist = false)
    private String authorNickname;

    @TableField(exist = false)
    private String authorAvatarUrl;

    @TableField(exist = false)
    private List<TagEntity> tags = List.of();
}
