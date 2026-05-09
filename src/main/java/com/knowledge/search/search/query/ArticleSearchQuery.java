package com.knowledge.search.search.query;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSearchQuery(
        String keyword,
        Long categoryId,
        List<String> tagNames,
        String status,
        String author,
        LocalDateTime publishTimeStart,
        LocalDateTime publishTimeEnd,
        LocalDateTime updatedTimeStart,
        LocalDateTime updatedTimeEnd,
        long pageNum,
        long pageSize,
        String sort,
        boolean highlight
) {
}
