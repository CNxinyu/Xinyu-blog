package com.xinyu.comment.service;

import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.service.ArticleService;
import com.xinyu.comment.dto.CommentCreateRequest;
import com.xinyu.comment.entity.CommentEntity;
import com.xinyu.comment.mapper.CommentMapper;
import com.xinyu.comment.model.CommentStatus;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {
    private CommentMapper commentMapper;
    private ArticleService articleService;
    private UserService userService;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        articleService = mock(ArticleService.class);
        userService = mock(UserService.class);
        commentService = new CommentService(commentMapper, articleService, userService);
    }

    @Test
    void createsPendingCommentForPublishedArticle() {
        when(articleService.requirePublishedById(3L)).thenReturn(new ArticleEntity());
        when(userService.requireById(7L)).thenReturn(user(7L));
        when(commentMapper.insert(any(CommentEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, CommentEntity.class).setId(11L);
            return 1;
        });

        CommentEntity result = commentService.create(3L, 7L,
                new CommentCreateRequest("  hello blog  ", null));

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getContent()).isEqualTo("hello blog");
        assertThat(result.getStatus()).isEqualTo(CommentStatus.PENDING.name());
        verify(articleService).requirePublishedById(3L);
    }

    @Test
    void statusUpdateUsesModerationStatus() {
        CommentEntity comment = new CommentEntity();
        comment.setId(11L);
        comment.setUserId(7L);
        comment.setStatus(CommentStatus.PENDING.name());
        when(commentMapper.selectById(11L)).thenReturn(comment);
        when(commentMapper.updateStatus(anyLong(), eq("APPROVED"), any())).thenAnswer(invocation -> {
            comment.setStatus(CommentStatus.APPROVED.name());
            return 1;
        });
        when(userService.requireById(7L)).thenReturn(user(7L));

        CommentEntity result = commentService.updateStatus(11L, CommentStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(CommentStatus.APPROVED.name());
        verify(commentMapper).updateStatus(any(), any(), any());
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setStatus("ACTIVE");
        return user;
    }
}
