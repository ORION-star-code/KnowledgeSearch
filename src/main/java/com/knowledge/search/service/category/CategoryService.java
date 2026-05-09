package com.knowledge.search.service.category;

import com.knowledge.search.domain.entity.CategoryEntity;
import com.knowledge.search.service.category.dto.CategoryCreateRequest;
import com.knowledge.search.service.category.dto.CategoryResponse;
import com.knowledge.search.service.category.dto.CategoryUpdateRequest;
import java.util.List;

public interface CategoryService {

    Long create(CategoryCreateRequest request);

    void update(Long id, CategoryUpdateRequest request);

    void delete(Long id);

    CategoryResponse getDetail(Long id);

    List<CategoryResponse> list();

    CategoryEntity getRequired(Long id);
}
