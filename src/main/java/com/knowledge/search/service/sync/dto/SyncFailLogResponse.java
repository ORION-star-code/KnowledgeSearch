package com.knowledge.search.service.sync.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SyncFailLogResponse(
        @Schema(description = "失败记录 ID", example = "1")
        Long id,
        @Schema(description = "业务类型", example = "ARTICLE")
        String bizType,
        @Schema(description = "业务 ID", example = "1")
        Long bizId,
        @Schema(description = "原始事件载荷")
        String payload,
        @Schema(description = "错误信息", example = "index unavailable")
        String errorMsg,
        @Schema(description = "已重试次数", example = "1")
        Integer retryCount,
        @Schema(description = "状态，可选值：PENDING/SUCCESS/FAILED", example = "PENDING")
        String status,
        @Schema(description = "下次重试时间")
        LocalDateTime nextRetryTime,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
