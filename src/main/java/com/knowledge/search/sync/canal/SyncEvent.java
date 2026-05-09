package com.knowledge.search.sync.canal;

import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncOperation;

public record SyncEvent(
        BizType bizType,
        Long bizId,
        SyncOperation operation,
        String payload
) {
}
