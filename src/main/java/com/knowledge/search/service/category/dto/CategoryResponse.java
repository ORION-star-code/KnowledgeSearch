package com.knowledge.search.service.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CategoryResponse(
        @Schema(description = "分类 ID", example = "1")
        Long id,
        @Schema(description = "分类名称", example = "Backend")
        String name,
        @Schema(description = "排序值", example = "1")
        Integer sort,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
