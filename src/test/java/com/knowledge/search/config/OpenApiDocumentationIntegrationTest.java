package com.knowledge.search.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeRootOpenApiDefinitionWithVersionField() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.paths").isMap());
    }

    @Test
    void shouldExposeSwaggerUiConfigWithGroupedApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configUrl").exists())
                .andExpect(jsonPath("$.urls").isArray());
    }

    @Test
    void shouldExposeSearchGroupWithUpdatedQueryParametersAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/search'].get").exists())
                .andExpect(jsonPath("$.paths['/search'].get.parameters[?(@.name=='author')]").exists())
                .andExpect(jsonPath("$.paths['/search'].get.parameters[?(@.name=='publishTimeStart')]").exists())
                .andExpect(jsonPath("$.paths['/search'].get.parameters[?(@.name=='publishTimeEnd')]").exists())
                .andExpect(jsonPath("$.paths['/search'].get.parameters[?(@.name=='updatedTimeStart')]").exists())
                .andExpect(jsonPath("$.paths['/search'].get.parameters[?(@.name=='updatedTimeEnd')]").exists())
                .andExpect(jsonPath("$.components.schemas").exists())
                .andExpect(jsonPath("$.components.schemas.SearchResponse").exists())
                .andExpect(jsonPath("$.components.schemas.SearchResultItem").exists());
    }

    @Test
    void shouldServeSwaggerUiPage() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }
}
