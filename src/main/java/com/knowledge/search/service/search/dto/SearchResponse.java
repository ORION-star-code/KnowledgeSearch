package com.knowledge.search.service.search.dto;

import com.knowledge.search.common.api.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SearchResponse", description = "搜索响应")
public record SearchResponse(
        @Schema(description = "搜索结果分页数据")
        PageResponse<SearchResultItem> page
) {
}
