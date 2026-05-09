package com.knowledge.search.service.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ArticlePublishRequest(
        @Schema(description = "发布时间", example = "2026-04-12T10:00:00")
        @NotNull LocalDateTime publishTime) {
}
