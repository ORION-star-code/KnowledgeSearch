package com.knowledge.search.controller.admin;

import com.knowledge.search.common.api.ApiResponse;
import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.service.sync.FullSyncService;
import com.knowledge.search.service.sync.SyncFailureService;
import com.knowledge.search.service.sync.SyncStatsResponse;
import com.knowledge.search.service.sync.dto.SyncFailLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/sync")
@Tag(name = "Admin Sync", description = "同步管理接口")
public class AdminSyncController {

    private final FullSyncService fullSyncService;
    private final SyncFailureService syncFailureService;

    @PostMapping("/full")
    @Operation(summary = "触发全量同步")
    public ApiResponse<String> fullSync() {
        return ApiResponse.success(fullSyncService.triggerFullSync());
    }

    @PostMapping("/retry/{failId}")
    @Operation(summary = "手动重试失败记录")
    public ApiResponse<Void> retry(@Parameter(description = "失败记录 ID", example = "1") @PathVariable Long failId) {
        syncFailureService.retry(failId);
        return ApiResponse.success(null);
    }

    @GetMapping("/fail/list")
    @Operation(summary = "分页查询同步失败记录")
    public ApiResponse<PageResponse<SyncFailLogResponse>> listFailures(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(syncFailureService.listFailures(pageNum, pageSize));
    }

    @GetMapping("/stats")
    @Operation(summary = "查询同步统计")
    public ApiResponse<SyncStatsResponse> stats() {
        return ApiResponse.success(syncFailureService.stats());
    }
}
