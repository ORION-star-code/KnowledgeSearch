package com.knowledge.search.sync.canal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncOperation;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanalEventAdapterImplTest {

    private final CanalEventAdapterImpl adapter = new CanalEventAdapterImpl();

    @Test
    void shouldMapArticleInsertToRebuildEvent() {
        CanalMessage message = new CanalMessage(
                "example",
                "knowledge_search",
                "kb_article",
                "INSERT",
                Map.of(),
                Map.of("id", "12"),
                LocalDateTime.now());

        SyncEvent event = adapter.adapt(message);

        assertEquals(BizType.ARTICLE, event.bizType());
        assertEquals(12L, event.bizId());
        assertEquals(SyncOperation.REBUILD, event.operation());
    }

    @Test
    void shouldUseArticleIdForArticleTagRelationEvents() {
        CanalMessage message = new CanalMessage(
                "example",
                "knowledge_search",
                "kb_article_tag",
                "DELETE",
                Map.of("id", "99", "article_id", "7", "tag_id", "3"),
                Map.of(),
                LocalDateTime.now());

        SyncEvent event = adapter.adapt(message);

        assertEquals(BizType.ARTICLE_TAG_RELATION, event.bizType());
        assertEquals(7L, event.bizId());
        assertEquals(SyncOperation.DELETE, event.operation());
    }

    @Test
    void shouldIgnoreUnknownTable() {
        CanalMessage message = new CanalMessage(
                "example",
                "knowledge_search",
                "kb_unknown",
                "UPDATE",
                Map.of("id", "1"),
                Map.of("id", "1"),
                LocalDateTime.now());

        assertNull(adapter.adapt(message));
    }

    @Test
    void shouldIgnoreMessageWithoutPrimaryKey() {
        CanalMessage message = new CanalMessage(
                "example",
                "knowledge_search",
                "kb_article",
                "UPDATE",
                Map.of(),
                Map.of(),
                LocalDateTime.now());

        assertNull(adapter.adapt(message));
    }
}
