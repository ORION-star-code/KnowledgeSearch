package com.knowledge.search.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应结构")
public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
