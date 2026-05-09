package com.knowledge.search.controller.search;

import com.knowledge.search.common.api.ApiResponse;
import com.knowledge.search.service.search.KnowledgeSearchService;
import com.knowledge.search.service.search.dto.SearchRequest;
import com.knowledge.search.service.search.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
@Tag(name = "Search", description = "知识库搜索接口")
public class SearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    @GetMapping
    @Operation(
            summary = "按关键字和过滤条件搜索文章",
            description = "支持标题、摘要、正文多字段搜索，可按分类、标签、状态、作者、发布时间和更新时间过滤，并返回高亮片段。"
    )
    public ApiResponse<SearchResponse> search(@Valid @ParameterObject @ModelAttribute SearchRequest request) {
        return ApiResponse.success(knowledgeSearchService.search(request));
    }
}
