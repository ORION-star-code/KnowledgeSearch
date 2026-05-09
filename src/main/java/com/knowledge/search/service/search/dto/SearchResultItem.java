package com.knowledge.search.service.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "SearchResultItem", description = "搜索结果项")
public record SearchResultItem(
        @Schema(description = "文章 ID", example = "1")
        Long id,
        @Schema(description = "标题", example = "Spring Guide")
        String title,
        @Schema(description = "标题高亮片段", example = "<em>Spring</em> Guide")
        String titleHighlight,
        @Schema(description = "摘要", example = "Guide summary")
        String summary,
        @Schema(description = "摘要高亮片段")
        String summaryHighlight,
        @Schema(description = "正文")
        String content,
        @Schema(description = "正文高亮片段")
        String contentHighlight,
        @Schema(description = "分类 ID", example = "1")
        Long categoryId,
        @Schema(description = "分类名称", example = "Backend")
        String categoryName,
        @Schema(description = "标签名称列表")
        List<String> tagNames,
        @Schema(description = "作者", example = "orion")
        String author,
        @Schema(description = "文章状态", example = "published")
        String status,
        @Schema(description = "发布时间")
        LocalDateTime publishTime,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
