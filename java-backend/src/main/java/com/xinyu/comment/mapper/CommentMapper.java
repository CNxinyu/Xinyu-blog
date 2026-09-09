package com.xinyu.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.comment.entity.CommentEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

public interface CommentMapper extends BaseMapper<CommentEntity> {
    @Results(id = "commentResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "articleId", column = "article_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "authorUsername", column = "author_username"),
            @Result(property = "authorNickname", column = "author_nickname"),
            @Result(property = "authorAvatarUrl", column = "author_avatar_url")
    })
    @Select("""
            SELECT c.id, c.article_id, c.user_id, c.parent_id, c.content, c.status,
                   c.created_at, c.updated_at,
                   u.username AS author_username, u.nickname AS author_nickname,
                   u.avatar_url AS author_avatar_url
            FROM comments c
            JOIN users u ON u.id = c.user_id
            WHERE c.article_id = #{articleId} AND c.status = 'APPROVED'
            ORDER BY c.created_at ASC, c.id ASC
            """)
    IPage<CommentEntity> selectApprovedPage(Page<?> page, @Param("articleId") Long articleId);

    @Results(id = "adminCommentResult", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "articleId", column = "article_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "authorUsername", column = "author_username"),
            @Result(property = "authorNickname", column = "author_nickname"),
            @Result(property = "authorAvatarUrl", column = "author_avatar_url")
    })
    @Select("""
            <script>
            SELECT c.id, c.article_id, c.user_id, c.parent_id, c.content, c.status,
                   c.created_at, c.updated_at,
                   u.username AS author_username, u.nickname AS author_nickname,
                   u.avatar_url AS author_avatar_url
            FROM comments c
            JOIN users u ON u.id = c.user_id
            WHERE 1 = 1
            <if test="status != null and status != ''">
              AND c.status = #{status}
            </if>
            <if test="articleId != null">
              AND c.article_id = #{articleId}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (c.content ILIKE CONCAT('%', #{keyword}, '%')
                   OR u.username ILIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY c.created_at ASC, c.id ASC
            </script>
            """)
    IPage<CommentEntity> selectAdminPage(Page<?> page,
                                          @Param("status") String status,
                                          @Param("articleId") Long articleId,
                                          @Param("keyword") String keyword);

    @Update("""
            UPDATE comments
            SET status = #{status}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("updatedAt") OffsetDateTime updatedAt);
}
