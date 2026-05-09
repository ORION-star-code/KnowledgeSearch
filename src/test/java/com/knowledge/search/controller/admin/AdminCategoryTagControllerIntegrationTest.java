package com.knowledge.search.controller.admin;

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
import com.knowledge.search.search.repository.ArticleIndexRepository;
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
class AdminCategoryTagControllerIntegrationTest {

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
    private ArticleIndexRepository articleIndexRepository;

    @BeforeEach
    void setUp() {
        articleIndexRepository.deleteAll();
        articleTagMapper.delete(new QueryWrapper<>());
        articleMapper.delete(new QueryWrapper<>());
        tagMapper.delete(new QueryWrapper<>());
        categoryMapper.delete(new QueryWrapper<>());
    }

    @Test
    void shouldSupportCategoryCrud() throws Exception {
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

        mockMvc.perform(get("/admin/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Backend"));

        mockMvc.perform(put("/admin/category/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Search","sort":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/admin/category/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Search"))
                .andExpect(jsonPath("$.data.sort").value(2));

        mockMvc.perform(delete("/admin/category/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectDeletingReferencedCategoryAndTag() throws Exception {
        long categoryId = extractId(mockMvc.perform(post("/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Backend","sort":1}
                                """))
                .andReturn().getResponse().getContentAsString());
        long tagId = extractId(mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Java"}
                                """))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/admin/article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Spring Boot Guide",
                                  "summary":"Quick summary",
                                  "content":"Long article content",
                                  "categoryId":%d,
                                  "author":"orion",
                                  "tagIds":[%d]
                                }
                                """.formatted(categoryId, tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/admin/category/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));

        mockMvc.perform(delete("/admin/tag/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TAG_IN_USE"));
    }

    private long extractId(String json) {
        String dataPart = json.replaceAll(".*\"data\":(\\d+).*", "$1");
        return Long.parseLong(dataPart);
    }
}
