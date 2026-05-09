package com.knowledge.search.service.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(
        @Schema(description = "标签名称", example = "Spring")
        @NotBlank String name) {
}
