package com.knowledge.search.controller.admin;

import com.knowledge.search.common.api.ApiResponse;
import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.service.article.ArticleService;
import com.knowledge.search.service.article.dto.ArticleCreateRequest;
import com.knowledge.search.service.article.dto.ArticleDetailResponse;
import com.knowledge.search.service.article.dto.ArticleListItemResponse;
import com.knowledge.search.service.article.dto.ArticlePageQuery;
import com.knowledge.search.service.article.dto.ArticlePublishRequest;
import com.knowledge.search.service.article.dto.ArticleUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/article")
@Tag(name = "Admin Article", description = "文章管理接口")
public class AdminArticleController {

    private final ArticleService articleService;

    @PostMapping
    @Operation(summary = "创建文章")
    public ApiResponse<Long> create(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResponse.success(articleService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看文章详情")
    public ApiResponse<ArticleDetailResponse> detail(@Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(articleService.getById(id));
    }

    @GetMapping
    @Operation(summary = "分页查询文章")
    public ApiResponse<PageResponse<ArticleListItemResponse>> list(@Valid @ModelAttribute ArticlePageQuery query) {
        return ApiResponse.success(articleService.list(query));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文章")
    public ApiResponse<Void> update(@Parameter(description = "文章 ID", example = "1") @PathVariable Long id,
                                    @Valid @RequestBody ArticleUpdateRequest request) {
        articleService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    public ApiResponse<Void> delete(@Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布文章")
    public ApiResponse<Void> publish(@Parameter(description = "文章 ID", example = "1") @PathVariable Long id,
                                     @Valid @RequestBody ArticlePublishRequest request) {
        articleService.publish(id, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/offline")
    @Operation(summary = "下线文章")
    public ApiResponse<Void> offline(@Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        articleService.offline(id);
        return ApiResponse.success(null);
    }
}
