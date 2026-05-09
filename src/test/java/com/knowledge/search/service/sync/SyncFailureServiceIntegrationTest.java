package com.knowledge.search.service.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncFailStatus;
import com.knowledge.search.common.enums.SyncOperation;
import com.knowledge.search.domain.entity.ArticleEntity;
import com.knowledge.search.domain.entity.ArticleTagEntity;
import com.knowledge.search.domain.entity.CategoryEntity;
import com.knowledge.search.domain.entity.SyncFailLogEntity;
import com.knowledge.search.domain.entity.TagEntity;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.mapper.CategoryMapper;
import com.knowledge.search.mapper.SyncFailLogMapper;
import com.knowledge.search.mapper.TagMapper;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.repository.ArticleIndexRepository;
import com.knowledge.search.sync.canal.SyncEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SyncFailureServiceIntegrationTest {

    @Autowired
    private IncrementalSyncService incrementalSyncService;

    @Autowired
    private SyncFailureService syncFailureService;

    @Autowired
    private SyncFailLogMapper syncFailLogMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @MockBean
    private ArticleIndexRepository articleIndexRepository;

    @BeforeEach
    void setUp() {
        syncFailLogMapper.delete(new QueryWrapper<>());
        articleTagMapper.delete(new QueryWrapper<>());
        articleMapper.delete(new QueryWrapper<>());
        tagMapper.delete(new QueryWrapper<>());
        categoryMapper.delete(new QueryWrapper<>());
        seedPublishedArticle();
        doNothing().when(articleIndexRepository).deleteById(any());
        doNothing().when(articleIndexRepository).deleteAll();
    }

    @Test
    void shouldRecordFailureAndMarkResolvedAfterManualRetry() {
        doThrow(new RuntimeException("index unavailable"))
                .when(articleIndexRepository)
                .save(any(ArticleDocument.class));

        assertThrows(RuntimeException.class,
                () -> incrementalSyncService.handle(new SyncEvent(BizType.ARTICLE, 1L, SyncOperation.UPDATE, null)));

        SyncFailLogEntity failLog = syncFailLogMapper.selectList(new QueryWrapper<>()).getFirst();
        assertEquals(SyncFailStatus.PENDING, failLog.getStatus());
        assertEquals(0, failLog.getRetryCount());

        Mockito.reset(articleIndexRepository);
        doNothing().when(articleIndexRepository).save(any(ArticleDocument.class));
        doNothing().when(articleIndexRepository).deleteById(any());
        doNothing().when(articleIndexRepository).deleteAll();

        syncFailureService.retry(failLog.getId());

        SyncFailLogEntity retried = syncFailLogMapper.selectById(failLog.getId());
        assertEquals(SyncFailStatus.SUCCESS, retried.getStatus());
    }

    private void seedPublishedArticle() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Backend");
        category.setSort(1);
        categoryMapper.insert(category);

        TagEntity tag = new TagEntity();
        tag.setName("Spring");
        tagMapper.insert(tag);

        ArticleEntity article = new ArticleEntity();
        article.setTitle("Spring Guide");
        article.setSummary("Guide summary");
        article.setContent("Guide content");
        article.setCategoryId(category.getId());
        article.setAuthor("orion");
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setIsDeleted(Boolean.FALSE);
        article.setPublishTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        articleMapper.insert(article);

        ArticleTagEntity relation = new ArticleTagEntity();
        relation.setArticleId(article.getId());
        relation.setTagId(tag.getId());
        articleTagMapper.insert(relation);
    }
}
