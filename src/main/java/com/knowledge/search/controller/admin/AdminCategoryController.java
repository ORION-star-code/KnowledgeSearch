package com.knowledge.search.controller.admin;

import com.knowledge.search.common.api.ApiResponse;
import com.knowledge.search.service.category.CategoryService;
import com.knowledge.search.service.category.dto.CategoryCreateRequest;
import com.knowledge.search.service.category.dto.CategoryResponse;
import com.knowledge.search.service.category.dto.CategoryUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/category")
@Tag(name = "Admin Category", description = "分类管理接口")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "创建分类")
    public ApiResponse<Long> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @GetMapping
    @Operation(summary = "查询分类列表")
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success(categoryService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看分类详情")
    public ApiResponse<CategoryResponse> detail(@Parameter(description = "分类 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(categoryService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    public ApiResponse<Void> update(@Parameter(description = "分类 ID", example = "1") @PathVariable Long id,
                                    @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@Parameter(description = "分类 ID", example = "1") @PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null);
    }
}
