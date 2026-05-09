package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record ArticlePageQuery(
        @Schema(description = "关键字，匹配标题/摘要/正文", example = "Spring")
        String keyword,
        @Schema(description = "文章状态，可选值：draft/published/offline", example = "published")
        String status,
        @Schema(description = "分类 ID", example = "1")
        Long categoryId,
        @Schema(description = "页码", example = "1", defaultValue = "1")
        @Min(1) Long pageNum,
        @Schema(description = "每页大小", example = "10", defaultValue = "10")
        @Min(1) Long pageSize
) {
    public ArticlePageQuery {
        pageNum = pageNum == null || pageNum == 0 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize == 0 ? 10 : pageSize;
    }
}
