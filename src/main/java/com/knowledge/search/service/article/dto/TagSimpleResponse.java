package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TagSimpleResponse(
        @Schema(description = "标签 ID", example = "1")
        Long id,
        @Schema(description = "标签名称", example = "Spring")
        String name) {
}
