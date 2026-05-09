package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ArticleCreateRequest(
        @Schema(description = "文章标题", example = "Spring Boot 接入指南")
        @NotBlank String title,
        @Schema(description = "文章摘要", example = "用于快速了解系统接入流程")
        String summary,
        @Schema(description = "文章正文", example = "这里是正文内容")
        @NotBlank String content,
        @Schema(description = "分类 ID", example = "1")
        @NotNull Long categoryId,
        @Schema(description = "作者", example = "orion")
        @NotBlank String author,
        @Schema(description = "标签 ID 列表", example = "[1,2]")
        List<Long> tagIds
) {
}
