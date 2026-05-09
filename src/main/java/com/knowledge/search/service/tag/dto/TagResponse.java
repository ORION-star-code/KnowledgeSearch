package com.knowledge.search.service.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TagResponse(
        @Schema(description = "标签 ID", example = "1")
        Long id,
        @Schema(description = "标签名称", example = "Spring")
        String name,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
