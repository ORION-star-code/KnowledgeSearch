package com.knowledge.search.service.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @Schema(description = "分类名称", example = "Search")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "排序值，越小越靠前", example = "2")
        Integer sort
) {
}
