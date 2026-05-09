package com.knowledge.search.service.sync;

import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.sync.canal.SyncEvent;
import com.knowledge.search.sync.worker.ArticleSyncWorker;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncrementalSyncServiceImpl implements IncrementalSyncService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleSyncWorker articleSyncWorker;
    private final ObjectProvider<SyncFailureService> syncFailureServiceProvider;

    @Override
    @Transactional(readOnly = true)
    public void handle(SyncEvent event) {
        dispatch(event, true);
    }

    void handleWithoutFailureRecord(SyncEvent event) {
        dispatch(event, false);
    }

    private void dispatch(SyncEvent event, boolean recordFailure) {
        if (event == null || event.bizId() == null || event.bizType() == null) {
            return;
        }

        switch (event.bizType()) {
            case ARTICLE -> executeSafely(event, () -> handleArticle(event), recordFailure);
            case CATEGORY -> rebuildArticles(articleMapper.selectIdsByCategoryId(event.bizId()), event.bizType(), recordFailure);
            case TAG -> rebuildArticles(articleTagMapper.selectArticleIdsByTagId(event.bizId()), event.bizType(), recordFailure);
            case ARTICLE_TAG_RELATION -> rebuildArticles(List.of(event.bizId()), event.bizType(), recordFailure);
        }
    }

    private void handleArticle(SyncEvent event) {
        if (event.operation() == com.knowledge.search.common.enums.SyncOperation.DELETE) {
            articleSyncWorker.delete(event.bizId());
            return;
        }
        articleSyncWorker.rebuildAndUpsert(event.bizId());
    }

    private void rebuildArticles(List<Long> articleIds, com.knowledge.search.common.enums.BizType sourceBizType, boolean recordFailure) {
        Set<Long> deduplicated = new LinkedHashSet<>(articleIds == null ? List.of() : articleIds);
        for (Long articleId : deduplicated) {
            SyncEvent rebuildEvent = new SyncEvent(
                    com.knowledge.search.common.enums.BizType.ARTICLE,
                    articleId,
                    com.knowledge.search.common.enums.SyncOperation.REBUILD,
                    sourceBizType.name());
            executeSafely(rebuildEvent, () -> articleSyncWorker.rebuildAndUpsert(articleId), recordFailure);
        }
    }

    private void executeSafely(SyncEvent event, Runnable action, boolean recordFailure) {
        try {
            action.run();
        } catch (Exception exception) {
            if (recordFailure) {
                syncFailureServiceProvider.getObject().recordFailure(event, exception);
            }
            throw exception;
        }
    }
}
