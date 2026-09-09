package com.xinyu.article.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleTagMapper {
    @Delete("DELETE FROM article_tags WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Insert("""
            <script>
            INSERT INTO article_tags (article_id, tag_id)
            VALUES
            <foreach collection="tagIds" item="tagId" separator=",">
                (#{articleId}, #{tagId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    @Select("""
            <script>
            SELECT at.article_id, t.id, t.name, t.slug
            FROM article_tags at
            JOIN tags t ON t.id = at.tag_id
            WHERE at.article_id IN
            <foreach collection="articleIds" item="articleId" open="(" separator="," close=")">
                #{articleId}
            </foreach>
            ORDER BY at.article_id ASC, t.name ASC, t.id ASC
            </script>
            """)
    List<ArticleTagRow> selectRowsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
