package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ArticleListItemResponse(
        @Schema(description = "文章 ID", example = "1")
        Long id,
        @Schema(description = "文章标题", example = "Spring Boot 接入指南")
        String title,
        @Schema(description = "文章摘要", example = "用于快速了解系统接入流程")
        String summary,
        @Schema(description = "作者", example = "orion")
        String author,
        @Schema(description = "文章状态", example = "published")
        String status,
        @Schema(description = "发布时间")
        LocalDateTime publishTime,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt,
        @Schema(description = "分类信息")
        CategorySimpleResponse category,
        @Schema(description = "标签列表")
        List<TagSimpleResponse> tags
) {
}
