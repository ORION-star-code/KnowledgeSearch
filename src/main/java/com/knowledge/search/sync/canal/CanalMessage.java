package com.knowledge.search.sync.canal;

import java.time.LocalDateTime;
import java.util.Map;

public record CanalMessage(
        String destination,
        String schemaName,
        String tableName,
        String eventType,
        Map<String, Object> before,
        Map<String, Object> after,
        LocalDateTime occurredAt
) {
}
