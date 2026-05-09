package com.knowledge.search.service.sync;

import io.swagger.v3.oas.annotations.media.Schema;

public record SyncStatsResponse(
        @Schema(description = "待重试数量", example = "2")
        long pending,
        @Schema(description = "已进入重试的数量", example = "1")
        long retrying,
        @Schema(description = "重试耗尽后的失败数量", example = "0")
        long failed,
        @Schema(description = "已恢复成功数量", example = "5")
        long resolved) {
}
