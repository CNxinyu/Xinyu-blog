package com.xinyu.article.service;

import com.xinyu.article.dto.ArticleCreateRequest;
import com.xinyu.article.dto.ArticleUpdateRequest;
import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.entity.CategoryEntity;
import com.xinyu.article.mapper.ArticleMapper;
import com.xinyu.article.mapper.ArticleTagMapper;
import com.xinyu.article.mapper.CategoryMapper;
import com.xinyu.common.exception.BusinessException;
import com.xinyu.user.entity.UserEntity;
import com.xinyu.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {
    private ArticleMapper articleMapper;
    private ArticleTagMapper articleTagMapper;
    private CategoryMapper categoryMapper;
    private TaxonomyService taxonomyService;
    private MarkdownRenderer markdownRenderer;
    private UserService userService;
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        articleTagMapper = mock(ArticleTagMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        taxonomyService = mock(TaxonomyService.class);
        markdownRenderer = mock(MarkdownRenderer.class);
        userService = mock(UserService.class);
        articleService = new ArticleService(articleMapper, articleTagMapper, categoryMapper,
                taxonomyService, markdownRenderer, userService);
    }

    @Test
    void createsDraftAndReplacesDistinctTags() {
        CategoryEntity category = category(10L);
        when(taxonomyService.requireCategory(10L)).thenReturn(category);
        when(articleMapper.selectBySlug("spring-security")).thenReturn(null);
        when(markdownRenderer.render("# JWT")).thenReturn("<h1 id=\"jwt\">JWT</h1>");
        when(articleMapper.insert(any(ArticleEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ArticleEntity.class).setId(99L);
            return 1;
        });
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(userService.requireById(7L)).thenReturn(user(7L));
        when(articleTagMapper.selectRowsByArticleIds(any())).thenReturn(List.of());

        ArticleEntity result = articleService.create(
                new ArticleCreateRequest("JWT Guide", "spring-security", "summary", 10L,
                        List.of(20L, 20L), "# JWT"), 7L);

        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getSlug()).isEqualTo("spring-security");
        assertThat(result.getContentHtml()).contains("<h1");
        verify(taxonomyService).requireTags(List.of(20L));
        verify(articleTagMapper).insertBatch(99L, List.of(20L));
    }

    @Test
    void rejectsDuplicateSlugBeforeInsert() {
        ArticleEntity existing = new ArticleEntity();
        existing.setId(2L);
        when(articleMapper.selectBySlug("existing")).thenReturn(existing);

        assertThatThrownBy(() -> articleService.create(
                new ArticleCreateRequest("Title", "existing", null, 1L, null, ""), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("article slug already exists");
        verify(articleMapper, never()).insert(any(ArticleEntity.class));
    }

    @Test
    void publishingRequiresNonEmptyMarkdown() {
        ArticleEntity article = article(1L, "DRAFT", "   ");
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(categoryMapper.selectById(10L)).thenReturn(category(10L));
        when(userService.requireById(7L)).thenReturn(user(7L));
        when(articleTagMapper.selectRowsByArticleIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> articleService.publish(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("title, category and non-empty content are required to publish");
        verify(articleMapper, never()).updatePublicationState(anyLong(), any(), any(), any(), any());
    }

    @Test
    void updateRegeneratesHtmlAndKeepsPublicationState() {
        ArticleEntity current = article(1L, "PUBLISHED", "old");
        when(articleMapper.selectById(1L)).thenReturn(current);
        when(articleMapper.selectBySlug("new-slug")).thenReturn(null);
        when(taxonomyService.requireCategory(10L)).thenReturn(category(10L));
        when(categoryMapper.selectById(10L)).thenReturn(category(10L));
        when(userService.requireById(7L)).thenReturn(user(7L));
        when(articleTagMapper.selectRowsByArticleIds(any())).thenReturn(List.of());
        when(markdownRenderer.render("new")).thenReturn("<p>new</p>");
        when(articleMapper.updateContent(eq(1L), eq("New"), eq("new-slug"), eq("summary"), eq(10L),
                eq("new"), eq("<p>new</p>"), any())).thenReturn(1);

        ArticleEntity result = articleService.update(1L,
                new ArticleUpdateRequest("New", "new-slug", "summary", 10L, null, "new"));

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getContentHtml()).isEqualTo("<p>new</p>");
        verify(articleTagMapper).deleteByArticleId(1L);
    }

    private CategoryEntity category(Long id) {
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setName("Java");
        category.setSlug("java");
        return category;
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("admin");
        user.setNickname("Admin");
        user.setStatus("ACTIVE");
        return user;
    }

    private ArticleEntity article(Long id, String status, String markdown) {
        ArticleEntity article = new ArticleEntity();
        article.setId(id);
        article.setAuthorId(7L);
        article.setCategoryId(10L);
        article.setTitle("Title");
        article.setSlug("slug");
        article.setContentMarkdown(markdown);
        article.setContentHtml("<p>old</p>");
        article.setStatus(status);
        return article;
    }
}
