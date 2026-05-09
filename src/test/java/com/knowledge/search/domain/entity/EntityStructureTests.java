package com.knowledge.search.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.enums.SyncFailStatus;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EntityStructureTests {

    @Test
    void articleStatusShouldMatchSqlValues() {
        assertEquals("draft", ArticleStatus.DRAFT.getValue());
        assertEquals("published", ArticleStatus.PUBLISHED.getValue());
        assertEquals("offline", ArticleStatus.OFFLINE.getValue());
    }

    @Test
    void syncFailStatusShouldMatchSqlValues() {
        assertEquals("PENDING", SyncFailStatus.PENDING.getValue());
        assertEquals("SUCCESS", SyncFailStatus.SUCCESS.getValue());
        assertEquals("FAILED", SyncFailStatus.FAILED.getValue());
    }

    @Test
    void articleTagEntityShouldNotContainUpdatedAt() {
        boolean hasUpdatedAt = Arrays.stream(ArticleTagEntity.class.getMethods())
                .anyMatch(method -> method.getName().equals("getUpdatedAt"));

        assertFalse(hasUpdatedAt);
    }
}
