package com.xinyu.article.service;

import com.xinyu.article.dto.CategoryUpsertRequest;
import com.xinyu.article.dto.TagUpsertRequest;
import com.xinyu.article.entity.CategoryEntity;
import com.xinyu.article.entity.TagEntity;
import com.xinyu.article.mapper.CategoryMapper;
import com.xinyu.article.mapper.TagMapper;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class TaxonomyService {
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public TaxonomyService(CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    @Transactional(readOnly = true)
    public CategoryEntity requireCategory(Long id) {
        CategoryEntity category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "category not found");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public List<CategoryEntity> categories() {
        return categoryMapper.selectAllOrderByName();
    }

    @Transactional
    public CategoryEntity createCategory(CategoryUpsertRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(normalizeDisplay(request.name()));
        entity.setSlug(normalizeSlug(request.slug()));
        entity.setDescription(trimToNull(request.description()));
        ensureCategoryAvailable(entity.getName(), entity.getSlug(), null);
        try {
            categoryMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "category already exists");
        }
        return entity;
    }

    @Transactional
    public CategoryEntity updateCategory(Long id, CategoryUpsertRequest request) {
        CategoryEntity current = requireCategory(id);
        String name = normalizeDisplay(request.name());
        String slug = normalizeSlug(request.slug());
        ensureCategoryAvailable(name, slug, id);
        OffsetDateTime now = now();
        int updated;
        try {
            updated = categoryMapper.updateCategory(id, name, slug, trimToNull(request.description()), now);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "category already exists");
        }
        if (updated != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "category not found");
        }
        current.setName(name);
        current.setSlug(slug);
        current.setDescription(trimToNull(request.description()));
        current.setUpdatedAt(now);
        return current;
    }

    @Transactional
    public void deleteCategory(Long id) {
        requireCategory(id);
        categoryMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TagEntity> requireTags(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<TagEntity> tags = distinctIds.stream().map(tagMapper::selectById).toList();
        if (tags.stream().anyMatch(tag -> tag == null)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "tag not found");
        }
        return tags;
    }

    @Transactional(readOnly = true)
    public List<TagEntity> tags() {
        return tagMapper.selectAllOrderByName();
    }

    @Transactional
    public TagEntity createTag(TagUpsertRequest request) {
        TagEntity entity = new TagEntity();
        entity.setName(normalizeDisplay(request.name()));
        entity.setSlug(normalizeSlug(request.slug()));
        ensureTagAvailable(entity.getName(), entity.getSlug(), null);
        try {
            tagMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "tag already exists");
        }
        return entity;
    }

    @Transactional
    public TagEntity updateTag(Long id, TagUpsertRequest request) {
        TagEntity current = tagMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "tag not found");
        }
        String name = normalizeDisplay(request.name());
        String slug = normalizeSlug(request.slug());
        ensureTagAvailable(name, slug, id);
        OffsetDateTime now = now();
        int updated;
        try {
            updated = tagMapper.updateTag(id, name, slug, now);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "tag already exists");
        }
        if (updated != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "tag not found");
        }
        current.setName(name);
        current.setSlug(slug);
        current.setUpdatedAt(now);
        return current;
    }

    @Transactional
    public void deleteTag(Long id) {
        if (tagMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "tag not found");
        }
        tagMapper.deleteById(id);
    }

    private void ensureCategoryAvailable(String name, String slug, Long currentId) {
        CategoryEntity byName = categoryMapper.selectByName(name);
        CategoryEntity bySlug = categoryMapper.selectBySlug(slug);
        if ((byName != null && !byName.getId().equals(currentId))
                || (bySlug != null && !bySlug.getId().equals(currentId))) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "category already exists");
        }
    }

    private void ensureTagAvailable(String name, String slug, Long currentId) {
        TagEntity byName = tagMapper.selectByName(name);
        TagEntity bySlug = tagMapper.selectBySlug(slug);
        if ((byName != null && !byName.getId().equals(currentId))
                || (bySlug != null && !bySlug.getId().equals(currentId))) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "tag already exists");
        }
    }

    private String normalizeDisplay(String value) {
        return value.trim();
    }

    public static String normalizeSlug(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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
