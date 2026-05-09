package com.knowledge.search.service.sync;

import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.service.sync.dto.SyncFailLogResponse;
import com.knowledge.search.sync.canal.SyncEvent;

public interface SyncFailureService {

    void recordFailure(SyncEvent event, Exception exception);

    void retry(Long failId);

    void retryPendingFailures();

    PageResponse<SyncFailLogResponse> listFailures(long pageNum, long pageSize);

    SyncStatsResponse stats();
}
