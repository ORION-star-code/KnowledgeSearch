package com.knowledge.search.service.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncOperation;
import com.knowledge.search.config.AppSyncProperties;
import com.knowledge.search.domain.entity.ArticleEntity;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.search.converter.ArticleDocumentBuilder;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.repository.ArticleIndexRepository;
import com.knowledge.search.sync.canal.SyncEvent;
import com.knowledge.search.sync.worker.ArticleSyncWorker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FullSyncServiceImpl implements FullSyncService {

    private final AppSyncProperties appSyncProperties;
    private final ArticleMapper articleMapper;
    private final ArticleDocumentBuilder articleDocumentBuilder;
    private final ArticleIndexRepository articleIndexRepository;
    private final ArticleSyncWorker articleSyncWorker;
    private final SyncFailureService syncFailureService;

    @Override
    public String triggerFullSync() {
        if (!appSyncProperties.full().enabled()) {
            log.warn("full sync skipped because app.sync.full.enabled=false");
            return "FULL_SYNC_DISABLED";
        }

        long start = System.currentTimeMillis();
        log.info("full sync started, pageSize={}", appSyncProperties.full().pageSize());
        articleIndexRepository.deleteAll();
        long current = 1L;
        long synced = 0L;
        Page<ArticleEntity> page;
        do {
            long pageQueryStart = System.currentTimeMillis();
            page = articleMapper.selectPage(
                    new Page<>(current, appSyncProperties.full().pageSize()),
                    new LambdaQueryWrapper<ArticleEntity>()
                            .eq(ArticleEntity::getIsDeleted, false)
                            .eq(ArticleEntity::getStatus, ArticleStatus.PUBLISHED)
                            .orderByAsc(ArticleEntity::getId));
            log.info("full sync page loaded, pageNo={}, records={}, totalPages={}, queryMs={}",
                    current, page.getRecords().size(), page.getPages(), System.currentTimeMillis() - pageQueryStart);
            synced += syncPage(page.getRecords());
            current++;
        } while (current <= page.getPages() && page.getPages() > 0);

        log.info("full sync completed, synced={}, totalMs={}", synced, System.currentTimeMillis() - start);
        return "FULL_SYNC_COMPLETED:" + synced;
    }

    private long syncPage(List<ArticleEntity> articles) {
        if (articles == null || articles.isEmpty()) {
            return 0L;
        }

        long pageStart = System.currentTimeMillis();
        Long firstArticleId = articles.getFirst().getId();
        Long lastArticleId = articles.getLast().getId();
        log.info("full sync page processing started, size={}, articleIdRange=[{}, {}]", articles.size(), firstArticleId, lastArticleId);
        try {
            long buildStart = System.currentTimeMillis();
            List<ArticleDocument> documents = articleDocumentBuilder.buildByArticles(articles);
            long buildMs = System.currentTimeMillis() - buildStart;
            log.info("full sync documents built, requested={}, built={}, buildMs={}",
                    articles.size(), documents.size(), buildMs);

            long saveStart = System.currentTimeMillis();
            articleIndexRepository.saveAll(documents);
            long saveMs = System.currentTimeMillis() - saveStart;
            log.info("full sync page saved to es, saved={}, saveMs={}, totalPageMs={}",
                    documents.size(), saveMs, System.currentTimeMillis() - pageStart);
            return documents.size();
        } catch (Exception batchException) {
            log.error("full sync batch path failed, size={}, articleIdRange=[{}, {}], fallback=single",
                    articles.size(), firstArticleId, lastArticleId, batchException);
            long synced = 0L;
            for (ArticleEntity article : articles) {
                try {
                    long singleStart = System.currentTimeMillis();
                    articleSyncWorker.rebuildAndUpsert(article.getId());
                    synced++;
                    log.info("full sync fallback single success, articleId={}, elapsedMs={}",
                            article.getId(), System.currentTimeMillis() - singleStart);
                } catch (Exception exception) {
                    log.error("full sync fallback single failed, articleId={}", article.getId(), exception);
                    syncFailureService.recordFailure(
                            new SyncEvent(BizType.ARTICLE, article.getId(), SyncOperation.REBUILD, "FULL_SYNC"),
                            exception);
                }
            }
            log.info("full sync fallback page finished, successCount={}, totalPageMs={}",
                    synced, System.currentTimeMillis() - pageStart);
            return synced;
        }
    }
}
