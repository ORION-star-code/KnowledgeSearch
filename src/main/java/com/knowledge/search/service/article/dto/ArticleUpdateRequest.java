package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ArticleUpdateRequest(
        @Schema(description = "文章标题", example = "Spring Boot 接入指南（更新）")
        @NotBlank String title,
        @Schema(description = "文章摘要", example = "更新后的摘要")
        String summary,
        @Schema(description = "文章正文", example = "更新后的正文")
        @NotBlank String content,
        @Schema(description = "分类 ID", example = "1")
        @NotNull Long categoryId,
        @Schema(description = "作者", example = "orion")
        @NotBlank String author,
        @Schema(description = "标签 ID 列表", example = "[1]")
        List<Long> tagIds
) {
}
