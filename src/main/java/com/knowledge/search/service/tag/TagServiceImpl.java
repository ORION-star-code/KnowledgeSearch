package com.knowledge.search.service.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.search.common.exception.BusinessException;
import com.knowledge.search.common.exception.ErrorCode;
import com.knowledge.search.domain.entity.TagEntity;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.mapper.TagMapper;
import com.knowledge.search.service.tag.dto.TagCreateRequest;
import com.knowledge.search.service.tag.dto.TagResponse;
import com.knowledge.search.service.tag.dto.TagUpdateRequest;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    @Override
    public Long create(TagCreateRequest request) {
        ensureNameUnique(request.name(), null);
        TagEntity entity = new TagEntity();
        entity.setName(request.name());
        tagMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, TagUpdateRequest request) {
        TagEntity entity = getRequired(id);
        ensureNameUnique(request.name(), id);
        entity.setName(request.name());
        tagMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getRequired(id);
        if (articleTagMapper.countByTagId(id) > 0) {
            throw new BusinessException(ErrorCode.TAG_IN_USE, "Tag is referenced by articles");
        }
        tagMapper.deleteById(id);
    }

    @Override
    public TagResponse getDetail(Long id) {
        return toResponse(getRequired(id));
    }

    @Override
    public List<TagResponse> list() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>()
                        .orderByAsc(TagEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TagEntity> getRequiredByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<TagEntity> tags = tagMapper.selectBatchIds(ids);
        if (tags.size() != ids.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND, "One or more tags do not exist");
        }
        return tags;
    }

    private TagEntity getRequired(Long id) {
        TagEntity entity = tagMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND, "Tag not found");
        }
        return entity;
    }

    private void ensureNameUnique(String name, Long excludeId) {
        TagEntity existing = tagMapper.selectOne(new LambdaQueryWrapper<TagEntity>()
                .eq(TagEntity::getName, name)
                .ne(excludeId != null, TagEntity::getId, excludeId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.TAG_NAME_EXISTS, "Tag name already exists");
        }
    }

    private TagResponse toResponse(TagEntity entity) {
        return new TagResponse(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
