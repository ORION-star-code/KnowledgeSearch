package com.knowledge.search.controller.admin;

import com.knowledge.search.common.api.ApiResponse;
import com.knowledge.search.service.tag.TagService;
import com.knowledge.search.service.tag.dto.TagCreateRequest;
import com.knowledge.search.service.tag.dto.TagResponse;
import com.knowledge.search.service.tag.dto.TagUpdateRequest;
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
@RequestMapping("/admin/tag")
@Tag(name = "Admin Tag", description = "标签管理接口")
public class AdminTagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "创建标签")
    public ApiResponse<Long> create(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.success(tagService.create(request));
    }

    @GetMapping
    @Operation(summary = "查询标签列表")
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.success(tagService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看标签详情")
    public ApiResponse<TagResponse> detail(@Parameter(description = "标签 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(tagService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    public ApiResponse<Void> update(@Parameter(description = "标签 ID", example = "1") @PathVariable Long id,
                                    @Valid @RequestBody TagUpdateRequest request) {
        tagService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public ApiResponse<Void> delete(@Parameter(description = "标签 ID", example = "1") @PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.success(null);
    }
}
