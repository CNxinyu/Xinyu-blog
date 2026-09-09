package com.xinyu.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.article.entity.ArticleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

public interface ArticleMapper extends BaseMapper<ArticleEntity> {
    @Select("SELECT id, author_id, category_id, title, slug, summary, content_markdown, "
            + "content_html, status, published_at, archived_at, created_at, updated_at "
            + "FROM articles WHERE LOWER(slug) = LOWER(#{slug}) LIMIT 1")
    ArticleEntity selectBySlug(@Param("slug") String slug);

    @Results(id = "articleResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "authorId", column = "author_id"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "slug", column = "slug"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "contentMarkdown", column = "content_markdown"),
            @Result(property = "contentHtml", column = "content_html"),
            @Result(property = "status", column = "status"),
            @Result(property = "publishedAt", column = "published_at"),
            @Result(property = "archivedAt", column = "archived_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "categorySlug", column = "category_slug"),
            @Result(property = "categoryDescription", column = "category_description"),
            @Result(property = "authorUsername", column = "author_username"),
            @Result(property = "authorNickname", column = "author_nickname"),
            @Result(property = "authorAvatarUrl", column = "author_avatar_url")
    })
    @Select("""
            <script>
            SELECT a.id, a.author_id, a.category_id, a.title, a.slug, a.summary,
                   a.content_markdown, a.content_html, a.status, a.published_at, a.archived_at,
                   a.created_at, a.updated_at,
                   c.name AS category_name, c.slug AS category_slug, c.description AS category_description,
                   u.username AS author_username, u.nickname AS author_nickname,
                   u.avatar_url AS author_avatar_url
            FROM articles a
            JOIN categories c ON c.id = a.category_id
            JOIN users u ON u.id = a.author_id
            WHERE a.status = 'PUBLISHED'
              AND a.slug = #{slug}
            LIMIT 1
            </script>
            """)
    ArticleEntity selectPublishedBySlug(@Param("slug") String slug);

    @Results(id = "articlePageResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "authorId", column = "author_id"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "slug", column = "slug"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "contentMarkdown", column = "content_markdown"),
            @Result(property = "contentHtml", column = "content_html"),
            @Result(property = "status", column = "status"),
            @Result(property = "publishedAt", column = "published_at"),
            @Result(property = "archivedAt", column = "archived_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "categorySlug", column = "category_slug"),
            @Result(property = "categoryDescription", column = "category_description"),
            @Result(property = "authorUsername", column = "author_username"),
            @Result(property = "authorNickname", column = "author_nickname"),
            @Result(property = "authorAvatarUrl", column = "author_avatar_url")
    })
    @Select("""
            <script>
            SELECT a.id, a.author_id, a.category_id, a.title, a.slug, a.summary,
                   a.content_markdown, a.content_html, a.status, a.published_at, a.archived_at,
                   a.created_at, a.updated_at,
                   c.name AS category_name, c.slug AS category_slug, c.description AS category_description,
                   u.username AS author_username, u.nickname AS author_nickname,
                   u.avatar_url AS author_avatar_url
            FROM articles a
            JOIN categories c ON c.id = a.category_id
            JOIN users u ON u.id = a.author_id
            WHERE a.status = 'PUBLISHED'
            <if test="keyword != null and keyword != ''">
              AND (
                   a.title ILIKE CONCAT('%', #{keyword}, '%')
                   OR COALESCE(a.summary, '') ILIKE CONCAT('%', #{keyword}, '%')
                   OR c.name ILIKE CONCAT('%', #{keyword}, '%')
                   OR EXISTS (
                       SELECT 1 FROM article_tags search_at
                       JOIN tags search_t ON search_t.id = search_at.tag_id
                       WHERE search_at.article_id = a.id
                         AND search_t.name ILIKE CONCAT('%', #{keyword}, '%')
                   )
              )
            </if>
            <if test="categoryId != null">
              AND a.category_id = #{categoryId}
            </if>
            <if test="tagId != null">
              AND EXISTS (
                  SELECT 1 FROM article_tags filter_at
                  WHERE filter_at.article_id = a.id AND filter_at.tag_id = #{tagId}
              )
            </if>
            ORDER BY a.published_at DESC NULLS LAST, a.id DESC
            </script>
            """)
    IPage<ArticleEntity> selectPublicPage(Page<?> page,
                                           @Param("keyword") String keyword,
                                           @Param("categoryId") Long categoryId,
                                           @Param("tagId") Long tagId);

    @Results(id = "articleAdminResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "authorId", column = "author_id"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "slug", column = "slug"),
            @Result(property = "summary", column = "summary"),
            @Result(property = "contentMarkdown", column = "content_markdown"),
            @Result(property = "contentHtml", column = "content_html"),
            @Result(property = "status", column = "status"),
            @Result(property = "publishedAt", column = "published_at"),
            @Result(property = "archivedAt", column = "archived_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "categoryName", column = "category_name"),
            @Result(property = "categorySlug", column = "category_slug"),
            @Result(property = "categoryDescription", column = "category_description"),
            @Result(property = "authorUsername", column = "author_username"),
            @Result(property = "authorNickname", column = "author_nickname"),
            @Result(property = "authorAvatarUrl", column = "author_avatar_url")
    })
    @Select("""
            <script>
            SELECT a.id, a.author_id, a.category_id, a.title, a.slug, a.summary,
                   a.content_markdown, a.content_html, a.status, a.published_at, a.archived_at,
                   a.created_at, a.updated_at,
                   c.name AS category_name, c.slug AS category_slug, c.description AS category_description,
                   u.username AS author_username, u.nickname AS author_nickname,
                   u.avatar_url AS author_avatar_url
            FROM articles a
            JOIN categories c ON c.id = a.category_id
            JOIN users u ON u.id = a.author_id
            WHERE 1 = 1
            <if test="status != null and status != ''">
              AND a.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                   a.title ILIKE CONCAT('%', #{keyword}, '%')
                   OR a.slug ILIKE CONCAT('%', #{keyword}, '%')
                   OR COALESCE(a.summary, '') ILIKE CONCAT('%', #{keyword}, '%')
                   OR c.name ILIKE CONCAT('%', #{keyword}, '%')
                   OR EXISTS (
                       SELECT 1 FROM article_tags search_at
                       JOIN tags search_t ON search_t.id = search_at.tag_id
                       WHERE search_at.article_id = a.id
                         AND search_t.name ILIKE CONCAT('%', #{keyword}, '%')
                   )
              )
            </if>
            <if test="categoryId != null">
              AND a.category_id = #{categoryId}
            </if>
            <if test="tagId != null">
              AND EXISTS (
                  SELECT 1 FROM article_tags filter_at
                  WHERE filter_at.article_id = a.id AND filter_at.tag_id = #{tagId}
              )
            </if>
            ORDER BY a.created_at DESC, a.id DESC
            </script>
            """)
    IPage<ArticleEntity> selectAdminPage(Page<?> page,
                                          @Param("status") String status,
                                          @Param("keyword") String keyword,
                                          @Param("categoryId") Long categoryId,
                                          @Param("tagId") Long tagId);

    @Update("""
            UPDATE articles
            SET title = #{title}, slug = #{slug}, summary = #{summary,jdbcType=VARCHAR},
                category_id = #{categoryId}, content_markdown = #{contentMarkdown},
                content_html = #{contentHtml}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateContent(@Param("id") Long id,
                      @Param("title") String title,
                      @Param("slug") String slug,
                      @Param("summary") String summary,
                      @Param("categoryId") Long categoryId,
                      @Param("contentMarkdown") String contentMarkdown,
                      @Param("contentHtml") String contentHtml,
                      @Param("updatedAt") OffsetDateTime updatedAt);

    @Update("""
            UPDATE articles
            SET status = #{status}, published_at = #{publishedAt}, archived_at = #{archivedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePublicationState(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("publishedAt") OffsetDateTime publishedAt,
                               @Param("archivedAt") OffsetDateTime archivedAt,
                               @Param("updatedAt") OffsetDateTime updatedAt);
}
