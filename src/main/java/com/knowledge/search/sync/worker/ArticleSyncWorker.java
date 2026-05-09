package com.knowledge.search.sync.worker;

import com.knowledge.search.search.converter.ArticleDocumentBuilder;
import com.knowledge.search.search.repository.ArticleIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSyncWorker {

    private final ArticleDocumentBuilder articleDocumentBuilder;
    private final ArticleIndexRepository articleIndexRepository;

    public void rebuildAndUpsert(Long articleId) {
        long start = System.currentTimeMillis();
        log.info("article sync worker rebuild started, articleId={}", articleId);
        var document = articleDocumentBuilder.buildByArticleId(articleId);
        if (document == null) {
            articleIndexRepository.deleteById(articleId);
            log.info("article sync worker rebuild resolved to delete, articleId={}, elapsedMs={}",
                    articleId, System.currentTimeMillis() - start);
            return;
        }
        articleIndexRepository.save(document);
        log.info("article sync worker rebuild saved, articleId={}, elapsedMs={}",
                articleId, System.currentTimeMillis() - start);
    }

    public void delete(Long articleId) {
        log.info("article sync worker delete, articleId={}", articleId);
        articleIndexRepository.deleteById(articleId);
    }
}
