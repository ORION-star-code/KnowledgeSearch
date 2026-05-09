package com.knowledge.search.service.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequest(
        @Schema(description = "分类名称", example = "Backend")
        @NotBlank String name,
        @Schema(description = "排序值，越小越靠前", example = "1")
        @Min(0) Integer sort
) {
}
