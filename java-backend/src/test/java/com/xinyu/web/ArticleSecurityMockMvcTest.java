package com.xinyu.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinyu.article.controller.AdminArticleController;
import com.xinyu.article.controller.ArticleController;
import com.xinyu.article.controller.SearchController;
import com.xinyu.article.controller.TaxonomyController;
import com.xinyu.article.dto.MarkdownPreviewResponse;
import com.xinyu.article.entity.ArticleEntity;
import com.xinyu.article.service.ArticleService;
import com.xinyu.article.service.TaxonomyService;
import com.xinyu.comment.controller.CommentController;
import com.xinyu.comment.entity.CommentEntity;
import com.xinyu.comment.service.CommentService;
import com.xinyu.common.security.SecurityConfig;
import com.xinyu.common.web.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ArticleController.class, AdminArticleController.class, SearchController.class,
        TaxonomyController.class, CommentController.class})
@Import({SecurityConfig.class, TraceIdFilter.class})
class ArticleSecurityMockMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private TaxonomyService taxonomyService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void publishedArticlesArePublic() throws Exception {
        when(articleService.publicPage(anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new Page<>(1, 20));

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void searchIsPublic() throws Exception {
        when(articleService.publicPage(anyInt(), anyInt(), eq("spring"), eq(null), eq(null)))
                .thenReturn(new Page<>(1, 20));

        mockMvc.perform(get("/api/search?q=spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void adminArticleListRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/articles").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice")
                                .roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPreviewMarkdown() throws Exception {
        when(articleService.preview("# Hello"))
                .thenReturn(new MarkdownPreviewResponse("<h1 id=\"hello\">Hello</h1>"));

        mockMvc.perform(post("/api/admin/articles/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMarkdown\":\"# Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentHtml").value("<h1 id=\"hello\">Hello</h1>"));
    }

    @Test
    void commentSubmissionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/articles/1/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanSubmitComment() throws Exception {
        CommentEntity comment = new CommentEntity();
        comment.setId(1L);
        comment.setArticleId(2L);
        comment.setUserId(7L);
        comment.setStatus("PENDING");
        comment.setContent("hello");
        when(commentService.create(eq(2L), eq(7L), any())).thenReturn(comment);

        mockMvc.perform(post("/api/articles/2/comments")
                        .with(jwt().jwt(token -> token.subject("7")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
