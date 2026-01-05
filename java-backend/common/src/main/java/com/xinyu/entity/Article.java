package com.xinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;  // 注意这里是 v3
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体类
 */
@Schema(description = "文章表")  // 替代 @ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("article")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "Spring Boot 最佳实践")
    private String title;

    @Schema(description = "文章摘要", example = "本文介绍了Spring Boot的常用技巧...")
    private String summary;

    @Schema(description = "文章内容（富文本或Markdown）")
    private String content;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者姓名（冗余字段，便于展示）", example = "张三")
    private String authorName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称（冗余字段）", example = "技术分享")
    private String categoryName;

    @Schema(description = "标签，多个用逗号分隔", example = "Java,Spring,Boot")
    private String tags;

    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "浏览量", example = "1024")
    private Integer viewCount;

    @Schema(description = "点赞数", example = "256")
    private Integer likeCount;

    @Schema(description = "评论数", example = "58")
    private Integer commentCount;

    @Schema(description = "是否置顶 1=是 0=否", example = "1")
    private Integer isTop;

    @Schema(description = "发布状态 1=已发布 0=草稿 -1=已删除", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除标志 1=已删除 0=正常")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}