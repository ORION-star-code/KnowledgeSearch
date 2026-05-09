package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategorySimpleResponse(
        @Schema(description = "分类 ID", example = "1")
        Long id,
        @Schema(description = "分类名称", example = "Backend")
        String name) {
}
