package com.knowledge.search.service.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.search.common.exception.BusinessException;
import com.knowledge.search.common.exception.ErrorCode;
import com.knowledge.search.domain.entity.CategoryEntity;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.CategoryMapper;
import com.knowledge.search.service.category.dto.CategoryCreateRequest;
import com.knowledge.search.service.category.dto.CategoryResponse;
import com.knowledge.search.service.category.dto.CategoryUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    @Override
    public Long create(CategoryCreateRequest request) {
        ensureNameUnique(request.name(), null);
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name());
        entity.setSort(request.sort() == null ? 0 : request.sort());
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, CategoryUpdateRequest request) {
        CategoryEntity entity = getRequired(id);
        ensureNameUnique(request.name(), id);
        entity.setName(request.name());
        entity.setSort(request.sort() == null ? 0 : request.sort());
        categoryMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getRequired(id);
        if (articleMapper.countByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE, "Category is referenced by articles");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public CategoryResponse getDetail(Long id) {
        return toResponse(getRequired(id));
    }

    @Override
    public List<CategoryResponse> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryEntity>()
                        .orderByAsc(CategoryEntity::getSort)
                        .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryEntity getRequired(Long id) {
        CategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found");
        }
        return entity;
    }

    private void ensureNameUnique(String name, Long excludeId) {
        CategoryEntity existing = categoryMapper.selectOne(new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getName, name)
                .ne(excludeId != null, CategoryEntity::getId, excludeId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS, "Category name already exists");
        }
    }

    private CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getSort(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
