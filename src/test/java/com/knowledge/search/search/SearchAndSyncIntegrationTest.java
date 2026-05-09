package com.knowledge.search.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncOperation;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.mapper.CategoryMapper;
import com.knowledge.search.mapper.SyncFailLogMapper;
import com.knowledge.search.mapper.TagMapper;
import com.knowledge.search.search.repository.ArticleIndexRepository;
import com.knowledge.search.service.sync.IncrementalSyncService;
import com.knowledge.search.sync.canal.SyncEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchAndSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncrementalSyncService incrementalSyncService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private SyncFailLogMapper syncFailLogMapper;

    @Autowired
    private ArticleIndexRepository articleIndexRepository;

    @BeforeEach
    void setUp() {
        articleIndexRepository.deleteAll();
        syncFailLogMapper.delete(new QueryWrapper<>());
        articleTagMapper.delete(new QueryWrapper<>());
        articleMapper.delete(new QueryWrapper<>());
        tagMapper.delete(new QueryWrapper<>());
        categoryMapper.delete(new QueryWrapper<>());
    }

    @Test
    void shouldSearchPublishedArticlesWithNativeStyleHighlightsAndHideOfflineArticlesAfterIncrementalSync() throws Exception {
        long categoryId = createCategory("Backend", 1);
        long tagId = createTag("Spring");
        long articleId = createArticle(
                "Spring Guide",
                "Guide summary",
                "Spring content body",
                categoryId,
                "orion",
                tagId);

        publishArticle(articleId, "2026-04-12T10:00:00");

        mockMvc.perform(post("/admin/sync/full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("FULL_SYNC_COMPLETED:1"));

        mockMvc.perform(get("/search")
                        .param("keyword", "Spring")
                        .param("tagNames", "Spring")
                        .param("highlight", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.total").value(1))
                .andExpect(jsonPath("$.data.page.records[0].title").value("Spring Guide"))
                .andExpect(jsonPath("$.data.page.records[0].titleHighlight").value("<em>Spring</em> Guide"))
                .andExpect(jsonPath("$.data.page.records[0].summaryHighlight").doesNotExist())
                .andExpect(jsonPath("$.data.page.records[0].contentHighlight").value("<em>Spring</em> content body"));

        mockMvc.perform(get("/search")
                        .param("keyword", "Spring")
                        .param("highlight", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.records[0].titleHighlight").doesNotExist())
                .andExpect(jsonPath("$.data.page.records[0].contentHighlight").doesNotExist());

        mockMvc.perform(put("/admin/article/{id}/offline", articleId))
                .andExpect(status().isOk());
        incrementalSyncService.handle(new SyncEvent(BizType.ARTICLE, articleId, SyncOperation.UPDATE, null));

        mockMvc.perform(get("/search")
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.total").value(0));
    }

    @Test
    void shouldFilterByAuthorAndTimeRangesAndSortLatestWhenKeywordIsAbsent() throws Exception {
        long backendCategoryId = createCategory("Backend", 1);
        long dataCategoryId = createCategory("Data", 2);
        long springTagId = createTag("Spring");
        long mysqlTagId = createTag("MySQL");

        long olderArticleId = createArticle(
                "Alpha Spring",
                "Alpha summary",
                "Alpha body",
                backendCategoryId,
                "orion",
                springTagId);
        long newerArticleId = createArticle(
                "Beta Spring",
                "Beta summary",
                "Beta body",
                backendCategoryId,
                "orion",
                springTagId);
        long otherAuthorArticleId = createArticle(
                "Gamma MySQL",
                "Gamma summary",
                "Gamma body",
                dataCategoryId,
                "guest",
                mysqlTagId);

        publishArticle(olderArticleId, "2026-04-11T10:00:00");
        publishArticle(newerArticleId, "2026-04-14T10:00:00");
        publishArticle(otherAuthorArticleId, "2026-04-13T10:00:00");

        mockMvc.perform(post("/admin/sync/full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("FULL_SYNC_COMPLETED:3"));

        mockMvc.perform(get("/search")
                        .param("author", "orion")
                        .param("categoryId", String.valueOf(backendCategoryId))
                        .param("tagNames", "Spring")
                        .param("publishTimeStart", "2026-04-12T00:00:00")
                        .param("updatedTimeStart", "2026-04-01T00:00:00")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.total").value(1))
                .andExpect(jsonPath("$.data.page.records[0].title").value("Beta Spring"))
                .andExpect(jsonPath("$.data.page.records[0].author").value("orion"));

        mockMvc.perform(get("/search")
                        .param("author", "orion")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.total").value(2))
                .andExpect(jsonPath("$.data.page.records[0].title").value("Beta Spring"))
                .andExpect(jsonPath("$.data.page.records[1].title").value("Alpha Spring"));
    }

    private long createCategory(String name, int sort) throws Exception {
        return extractId(mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","sort":%d}
                                """.formatted(name, sort)))
                .andReturn().getResponse().getContentAsString());
    }

    private long createTag(String name) throws Exception {
        return extractId(mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andReturn().getResponse().getContentAsString());
    }

    private long createArticle(String title, String summary, String content, long categoryId, String author, long tagId)
            throws Exception {
        return extractId(mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "summary":"%s",
                                  "content":"%s",
                                  "categoryId":%d,
                                  "author":"%s",
                                  "tagIds":[%d]
                                }
                                """.formatted(title, summary, content, categoryId, author, tagId)))
                .andReturn().getResponse().getContentAsString());
    }

    private void publishArticle(long articleId, String publishTime) throws Exception {
        mockMvc.perform(put("/admin/article/{id}/publish", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishTime":"%s"}
                                """.formatted(publishTime)))
                .andExpect(status().isOk());
    }

    private long extractId(String json) {
        String dataPart = json.replaceAll(".*\"data\":(\\d+).*", "$1");
        return Long.parseLong(dataPart);
    }
}
