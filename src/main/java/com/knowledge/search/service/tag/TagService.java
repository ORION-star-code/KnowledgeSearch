package com.knowledge.search.service.tag;

import com.knowledge.search.domain.entity.TagEntity;
import com.knowledge.search.service.tag.dto.TagCreateRequest;
import com.knowledge.search.service.tag.dto.TagResponse;
import com.knowledge.search.service.tag.dto.TagUpdateRequest;
import java.util.Collection;
import java.util.List;

public interface TagService {

    Long create(TagCreateRequest request);

    void update(Long id, TagUpdateRequest request);

    void delete(Long id);

    TagResponse getDetail(Long id);

    List<TagResponse> list();

    List<TagEntity> getRequiredByIds(Collection<Long> ids);
}
