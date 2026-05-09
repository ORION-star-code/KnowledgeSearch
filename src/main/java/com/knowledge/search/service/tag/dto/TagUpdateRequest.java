package com.knowledge.search.service.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpdateRequest(
        @Schema(description = "标签名称", example = "Java")
        @NotBlank @Size(max = 100) String name
) {
}
