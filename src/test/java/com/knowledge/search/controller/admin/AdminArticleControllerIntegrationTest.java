package com.knowledge.search.controller.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.mapper.CategoryMapper;
import com.knowledge.search.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminArticleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        articleTagMapper.delete(new QueryWrapper<>());
        articleMapper.delete(new QueryWrapper<>());
        tagMapper.delete(new QueryWrapper<>());
        categoryMapper.delete(new QueryWrapper<>());
    }

    @Test
    void shouldCreateReadPublishOfflineListAndDeleteArticle() throws Exception {
        String categoryResponse = mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Backend","sort":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long categoryId = extractId(categoryResponse);

        String tagOneResponse = mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Java"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tagTwoResponse = mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Spring"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long tagOneId = extractId(tagOneResponse);
        long tagTwoId = extractId(tagTwoResponse);

        String articleResponse = mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Spring Boot Guide",
                                  "summary":"Quick summary",
                                  "content":"Long article content",
                                  "categoryId":%d,
                                  "author":"orion",
                                  "tagIds":[%d,%d]
                                }
                                """.formatted(categoryId, tagOneId, tagTwoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long articleId = extractId(articleResponse);

        mockMvc.perform(get("/admin/article/{id}", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.data.category.name").value("Backend"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)));

        mockMvc.perform(put("/admin/article/{id}", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Spring Boot Guide Updated",
                                  "summary":"Updated summary",
                                  "content":"Updated content",
                                  "categoryId":%d,
                                  "author":"orion",
                                  "tagIds":[%d]
                                }
                                """.formatted(categoryId, tagOneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/admin/article/{id}/publish", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishTime":"2026-04-12T10:00:00"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/article")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Spring Boot Guide Updated"));

        mockMvc.perform(put("/admin/article/{id}/offline", articleId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/admin/article/{id}", articleId))
                .andExpect(status().isOk());

        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM kb_article WHERE id = ?",
                Boolean.class,
                articleId);
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, deleted);
        org.junit.jupiter.api.Assertions.assertTrue(articleTagMapper.selectByArticleIds(java.util.List.of(articleId)).isEmpty());
    }

    @Test
    void shouldRejectCreateWhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Invalid",
                                  "summary":"Invalid",
                                  "content":"Invalid",
                                  "categoryId":999,
                                  "author":"orion",
                                  "tagIds":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    private long extractId(String json) {
        String dataPart = json.replaceAll(".*\"data\":(\\d+).*", "$1");
        return Long.parseLong(dataPart);
    }
}
