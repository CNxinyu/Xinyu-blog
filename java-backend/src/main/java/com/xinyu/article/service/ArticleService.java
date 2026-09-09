package com.xinyu.article.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.article.dto.ArticleCreateRequest;
import com.xinyu.article.dto.ArticleDetailResponse;
import com.xinyu.article.dto.ArticleUpdateRequest;
import com.xinyu.article.dto.MarkdownPreviewResponse;
import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.entity.CategoryEntity;
import com.xinyu.article.entity.TagEntity;
import com.xinyu.article.mapper.ArticleMapper;
import com.xinyu.article.mapper.ArticleTagMapper;
import com.xinyu.article.mapper.ArticleTagRow;
import com.xinyu.article.mapper.CategoryMapper;
import com.xinyu.article.model.ArticleStatus;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TaxonomyService taxonomyService;
    private final MarkdownRenderer markdownRenderer;
    private final UserService userService;

    public ArticleService(ArticleMapper articleMapper, ArticleTagMapper articleTagMapper,
                          CategoryMapper categoryMapper, TaxonomyService taxonomyService,
                          MarkdownRenderer markdownRenderer, UserService userService) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.categoryMapper = categoryMapper;
        this.taxonomyService = taxonomyService;
        this.markdownRenderer = markdownRenderer;
        this.userService = userService;
    }

    @Transactional
    public ArticleEntity create(ArticleCreateRequest request, Long authorId) {
        CategoryEntity category = taxonomyService.requireCategory(request.categoryId());
        List<Long> tagIds = distinctIds(request.tagIds());
        taxonomyService.requireTags(tagIds);
        String slug = TaxonomyService.normalizeSlug(request.slug());
        ensureSlugAvailable(slug, null);

        ArticleEntity entity = new ArticleEntity();
        entity.setAuthorId(authorId);
        entity.setCategoryId(category.getId());
        entity.setTitle(request.title().trim());
        entity.setSlug(slug);
        entity.setSummary(trimToNull(request.summary()));
        entity.setContentMarkdown(content(request.contentMarkdown()));
        entity.setContentHtml(markdownRenderer.render(entity.getContentMarkdown()));
        entity.setStatus(ArticleStatus.DRAFT.name());
        try {
            articleMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "article slug already exists");
        }
        replaceTags(entity.getId(), tagIds);
        return hydrate(entity);
    }

    @Transactional
    public ArticleEntity update(Long id, ArticleUpdateRequest request) {
        ArticleEntity current = requireAdmin(id);
        String slug = TaxonomyService.normalizeSlug(request.slug());
        ensureSlugAvailable(slug, id);
        CategoryEntity category = taxonomyService.requireCategory(request.categoryId());
        List<Long> tagIds = distinctIds(request.tagIds());
        taxonomyService.requireTags(tagIds);

        String markdown = content(request.contentMarkdown());
        OffsetDateTime updatedAt = now();
        try {
            if (articleMapper.updateContent(id, request.title().trim(), slug, trimToNull(request.summary()),
                    category.getId(), markdown, markdownRenderer.render(markdown), updatedAt) != 1) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "article slug already exists");
        }
        replaceTags(id, tagIds);
        current.setTitle(request.title().trim());
        current.setSlug(slug);
        current.setSummary(trimToNull(request.summary()));
        current.setCategoryId(category.getId());
        current.setContentMarkdown(markdown);
        current.setContentHtml(markdownRenderer.render(markdown));
        current.setUpdatedAt(updatedAt);
        return hydrate(current);
    }

    @Transactional
    public ArticleEntity saveDraft(Long id) {
        ArticleEntity current = requireAdmin(id);
        OffsetDateTime updatedAt = now();
        updatePublicationState(id, ArticleStatus.DRAFT, null, null, updatedAt);
        current.setStatus(ArticleStatus.DRAFT.name());
        current.setPublishedAt(null);
        current.setArchivedAt(null);
        current.setUpdatedAt(updatedAt);
        return hydrate(current);
    }

    @Transactional
    public ArticleEntity publish(Long id) {
        ArticleEntity current = requireAdmin(id);
        if (!StringUtils.hasText(current.getTitle())
                || current.getCategoryId() == null
                || !StringUtils.hasText(current.getContentMarkdown())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "title, category and non-empty content are required to publish");
        }
        OffsetDateTime publishedAt = current.getPublishedAt() == null ? now() : current.getPublishedAt();
        OffsetDateTime updatedAt = now();
        updatePublicationState(id, ArticleStatus.PUBLISHED, publishedAt, null, updatedAt);
        current.setStatus(ArticleStatus.PUBLISHED.name());
        current.setPublishedAt(publishedAt);
        current.setArchivedAt(null);
        current.setUpdatedAt(updatedAt);
        return hydrate(current);
    }

    @Transactional
    public ArticleEntity archive(Long id) {
        ArticleEntity current = requireAdmin(id);
        OffsetDateTime updatedAt = now();
        updatePublicationState(id, ArticleStatus.ARCHIVED, current.getPublishedAt(), now(), updatedAt);
        current.setStatus(ArticleStatus.ARCHIVED.name());
        current.setArchivedAt(updatedAt);
        current.setUpdatedAt(updatedAt);
        return hydrate(current);
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin(id);
        if (articleMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
        }
    }

    @Transactional(readOnly = true)
    public ArticleEntity requireAdmin(Long id) {
        ArticleEntity entity = articleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
        }
        return hydrate(entity);
    }

    @Transactional(readOnly = true)
    public ArticleEntity requirePublishedBySlug(String slug) {
        ArticleEntity entity = articleMapper.selectPublishedBySlug(TaxonomyService.normalizeSlug(slug));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
        }
        attachTags(List.of(entity));
        return entity;
    }

    @Transactional(readOnly = true)
    public ArticleEntity requirePublishedById(Long id) {
        ArticleEntity entity = articleMapper.selectById(id);
        if (entity == null || !ArticleStatus.PUBLISHED.name().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
        }
        return hydrate(entity);
    }

    @Transactional(readOnly = true)
    public IPage<ArticleEntity> publicPage(int page, int size, String keyword, Long categoryId, Long tagId) {
        IPage<ArticleEntity> result = articleMapper.selectPublicPage(
                new Page<>(page, size), trimToNull(keyword), categoryId, tagId);
        attachTags(result.getRecords());
        return result;
    }

    @Transactional(readOnly = true)
    public IPage<ArticleEntity> adminPage(int page, int size, ArticleStatus status,
                                          String keyword, Long categoryId, Long tagId) {
        IPage<ArticleEntity> result = articleMapper.selectAdminPage(
                new Page<>(page, size), status == null ? null : status.name(), trimToNull(keyword), categoryId, tagId);
        attachTags(result.getRecords());
        return result;
    }

    @Transactional(readOnly = true)
    public MarkdownPreviewResponse preview(String markdown) {
        return new MarkdownPreviewResponse(markdownRenderer.render(content(markdown)));
    }

    private void updatePublicationState(Long id, ArticleStatus status, OffsetDateTime publishedAt,
                                        OffsetDateTime archivedAt, OffsetDateTime updatedAt) {
        if (articleMapper.updatePublicationState(id, status.name(), publishedAt, archivedAt, updatedAt) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "article not found");
        }
    }

    private void replaceTags(Long articleId, Collection<Long> tagIds) {
        articleTagMapper.deleteByArticleId(articleId);
        if (!tagIds.isEmpty()) {
            articleTagMapper.insertBatch(articleId, tagIds.stream().toList());
        }
    }

    private void ensureSlugAvailable(String slug, Long currentId) {
        ArticleEntity existing = articleMapper.selectBySlug(slug);
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "article slug already exists");
        }
    }

    private ArticleEntity hydrate(ArticleEntity entity) {
        if (entity.getCategoryId() != null && entity.getCategoryName() == null) {
            CategoryEntity category = categoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                entity.setCategoryName(category.getName());
                entity.setCategorySlug(category.getSlug());
                entity.setCategoryDescription(category.getDescription());
            }
        }
        if (entity.getAuthorId() != null && entity.getAuthorUsername() == null) {
            UserEntity author = userService.requireById(entity.getAuthorId());
            entity.setAuthorUsername(author.getUsername());
            entity.setAuthorNickname(author.getNickname());
            entity.setAuthorAvatarUrl(author.getAvatarUrl());
        }
        attachTags(List.of(entity));
        return entity;
    }

    private void attachTags(List<ArticleEntity> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Long> articleIds = articles.stream().map(ArticleEntity::getId).filter(Objects::nonNull).toList();
        if (articleIds.isEmpty()) {
            return;
        }
        Map<Long, List<TagEntity>> tagsByArticle = articleTagMapper.selectRowsByArticleIds(articleIds).stream()
                .collect(Collectors.groupingBy(ArticleTagRow::getArticleId,
                        Collectors.mapping(this::toTag, Collectors.toList())));
        articles.forEach(article -> article.setTags(tagsByArticle.getOrDefault(article.getId(), List.of())));
    }

    private TagEntity toTag(ArticleTagRow row) {
        TagEntity tag = new TagEntity();
        tag.setId(row.getId());
        tag.setName(row.getName());
        tag.setSlug(row.getSlug());
        return tag;
    }

    private List<Long> distinctIds(Collection<Long> ids) {
        return ids == null ? Collections.emptyList() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private String content(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
