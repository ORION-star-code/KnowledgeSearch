package com.knowledge.search.sync.canal;

import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncOperation;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CanalEventAdapterImpl implements CanalEventAdapter {

    @Override
    public SyncEvent adapt(CanalMessage message) {
        if (message == null || !StringUtils.hasText(message.tableName()) || !StringUtils.hasText(message.eventType())) {
            return null;
        }

        BizType bizType = mapBizType(message.tableName());
        if (bizType == null) {
            return null;
        }

        Long bizId = resolveBizId(message, bizType);
        if (bizId == null) {
            return null;
        }

        return new SyncEvent(
                bizType,
                bizId,
                mapOperation(message.eventType()),
                message.tableName() + ":" + message.eventType());
    }

    private BizType mapBizType(String tableName) {
        return switch (tableName) {
            case "kb_article" -> BizType.ARTICLE;
            case "kb_category" -> BizType.CATEGORY;
            case "kb_tag" -> BizType.TAG;
            case "kb_article_tag" -> BizType.ARTICLE_TAG_RELATION;
            default -> null;
        };
    }

    private SyncOperation mapOperation(String eventType) {
        return "DELETE".equalsIgnoreCase(eventType) ? SyncOperation.DELETE : SyncOperation.REBUILD;
    }

    private Long resolveBizId(CanalMessage message, BizType bizType) {
        String primaryKey = bizType == BizType.ARTICLE_TAG_RELATION ? "article_id" : "id";
        Map<String, Object> preferred = "DELETE".equalsIgnoreCase(message.eventType()) ? message.before() : message.after();
        Map<String, Object> fallback = "DELETE".equalsIgnoreCase(message.eventType()) ? message.after() : message.before();
        Long bizId = readLong(preferred, primaryKey);
        return bizId != null ? bizId : readLong(fallback, primaryKey);
    }

    private Long readLong(Map<String, Object> values, String key) {
        if (values == null || !values.containsKey(key) || values.get(key) == null) {
            return null;
        }
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? Long.valueOf(text) : null;
    }
}
