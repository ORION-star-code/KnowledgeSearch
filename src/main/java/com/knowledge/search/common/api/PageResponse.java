package com.knowledge.search.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页响应结构")
public record PageResponse<T>(long pageNum, long pageSize, long total, List<T> records) {

    public static <T> PageResponse<T> empty(long pageNum, long pageSize) {
        return new PageResponse<>(pageNum, pageSize, 0L, List.of());
    }
}
