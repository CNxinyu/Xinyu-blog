package com.xinyu.comment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.article.service.ArticleService;
import com.xinyu.comment.dto.CommentCreateRequest;
import com.xinyu.comment.entity.CommentEntity;
import com.xinyu.comment.mapper.CommentMapper;
import com.xinyu.comment.model.CommentStatus;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CommentService {
    private final CommentMapper commentMapper;
    private final ArticleService articleService;
    private final UserService userService;

    public CommentService(CommentMapper commentMapper, ArticleService articleService, UserService userService) {
        this.commentMapper = commentMapper;
        this.articleService = articleService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public IPage<CommentEntity> publicPage(Long articleId, int page, int size) {
        articleService.requirePublishedById(articleId);
        return commentMapper.selectApprovedPage(new Page<>(page, size), articleId);
    }

    @Transactional
    public CommentEntity create(Long articleId, Long userId, CommentCreateRequest request) {
        articleService.requirePublishedById(articleId);
        UserEntity user = requireActiveUser(userId);
        if (request.parentId() != null) {
            CommentEntity parent = commentMapper.selectById(request.parentId());
            if (parent == null || !articleId.equals(parent.getArticleId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "parent comment not found");
            }
        }

        CommentEntity entity = new CommentEntity();
        entity.setArticleId(articleId);
        entity.setUserId(userId);
        entity.setParentId(request.parentId());
        entity.setContent(request.content().trim());
        entity.setStatus(CommentStatus.PENDING.name());
        commentMapper.insert(entity);
        entity.setAuthorUsername(user.getUsername());
        entity.setAuthorNickname(user.getNickname());
        entity.setAuthorAvatarUrl(user.getAvatarUrl());
        return entity;
    }

    @Transactional(readOnly = true)
    public IPage<CommentEntity> adminPage(int page, int size, CommentStatus status,
                                          Long articleId, String keyword) {
        IPage<CommentEntity> result = commentMapper.selectAdminPage(
                new Page<>(page, size), status == null ? null : status.name(), articleId,
                StringUtils.hasText(keyword) ? keyword.trim() : null);
        return result;
    }

    @Transactional
    public CommentEntity updateStatus(Long id, CommentStatus status) {
        require(id);
        OffsetDateTime updatedAt = now();
        if (commentMapper.updateStatus(id, status.name(), updatedAt) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
        }
        return withAuthor(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (commentMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
        }
    }

    private CommentEntity require(Long id) {
        CommentEntity entity = commentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
        }
        return entity;
    }

    private CommentEntity withAuthor(CommentEntity entity) {
        UserEntity user = userService.requireById(entity.getUserId());
        entity.setAuthorUsername(user.getUsername());
        entity.setAuthorNickname(user.getNickname());
        entity.setAuthorAvatarUrl(user.getAvatarUrl());
        return entity;
    }

    private UserEntity requireActiveUser(Long userId) {
        UserEntity user = userService.requireById(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
