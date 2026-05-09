package com.knowledge.search.service.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(name = "SearchRequest", description = "搜索请求参数")
public record SearchRequest(
        @Schema(description = "关键字，匹配标题/摘要/正文", example = "Spring Boot")
        String keyword,
        @Schema(description = "分类 ID", example = "1")
        Long categoryId,
        @Schema(description = "标签名称列表", example = "[\"Spring\",\"Java\"]")
        List<String> tagNames,
        @Schema(description = "文章状态，取值：draft/published/offline", example = "published")
        String status,
        @Schema(description = "作者", example = "orion")
        String author,
        @Schema(description = "发布时间开始，ISO-8601 格式", example = "2026-04-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime publishTimeStart,
        @Schema(description = "发布时间结束，ISO-8601 格式", example = "2026-04-30T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime publishTimeEnd,
        @Schema(description = "更新时间开始，ISO-8601 格式", example = "2026-04-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime updatedTimeStart,
        @Schema(description = "更新时间结束，ISO-8601 格式", example = "2026-04-30T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime updatedTimeEnd,
        @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
        @Min(1)
        Long pageNum,
        @Schema(description = "每页数量", example = "10", defaultValue = "10")
        @Min(1)
        Long pageSize,
        @Schema(description = "排序方式，relevance(默认)/latest", example = "relevance", defaultValue = "relevance")
        String sort,
        @Schema(description = "是否返回高亮片段", example = "true", defaultValue = "true")
        Boolean highlight
) {
    public SearchRequest {
        pageNum = pageNum == null || pageNum == 0 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize == 0 ? 10 : pageSize;
        highlight = highlight == null ? Boolean.TRUE : highlight;
    }
}
