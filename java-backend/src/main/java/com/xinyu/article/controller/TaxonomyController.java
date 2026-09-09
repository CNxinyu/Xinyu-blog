package com.xinyu.article.controller;

import com.xinyu.article.dto.CategoryResponse;
import com.xinyu.article.dto.CategoryUpsertRequest;
import com.xinyu.article.dto.TagResponse;
import com.xinyu.article.dto.TagUpsertRequest;
import com.xinyu.article.service.TaxonomyService;
import com.xinyu.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Taxonomy", description = "Article categories and tags")
public class TaxonomyController {
    private final TaxonomyService taxonomyService;

    public TaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    public ApiResponse<java.util.List<CategoryResponse>> categories() {
        return ApiResponse.success(taxonomyService.categories().stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/tags")
    @Operation(summary = "List tags")
    public ApiResponse<java.util.List<TagResponse>> tags() {
        return ApiResponse.success(taxonomyService.tags().stream().map(TagResponse::from).toList());
    }

    @PostMapping("/admin/categories")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        return ApiResponse.success(CategoryResponse.from(taxonomyService.createCategory(request)));
    }

    @PutMapping("/admin/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody CategoryUpsertRequest request) {
        return ApiResponse.success(CategoryResponse.from(taxonomyService.updateCategory(id, request)));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable @Positive Long id) {
        taxonomyService.deleteCategory(id);
        return ApiResponse.success();
    }

    @PostMapping("/admin/tags")
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagUpsertRequest request) {
        return ApiResponse.success(TagResponse.from(taxonomyService.createTag(request)));
    }

    @PutMapping("/admin/tags/{id}")
    public ApiResponse<TagResponse> updateTag(@PathVariable @Positive Long id,
                                              @Valid @RequestBody TagUpsertRequest request) {
        return ApiResponse.success(TagResponse.from(taxonomyService.updateTag(id, request)));
    }

    @DeleteMapping("/admin/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable @Positive Long id) {
        taxonomyService.deleteTag(id);
        return ApiResponse.success();
    }
}
