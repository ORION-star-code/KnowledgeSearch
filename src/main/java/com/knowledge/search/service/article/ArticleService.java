package com.knowledge.search.service.article;

import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.service.article.dto.ArticleDetailResponse;
import com.knowledge.search.service.article.dto.ArticleCreateRequest;
import com.knowledge.search.service.article.dto.ArticleListItemResponse;
import com.knowledge.search.service.article.dto.ArticlePageQuery;
import com.knowledge.search.service.article.dto.ArticlePublishRequest;
import com.knowledge.search.service.article.dto.ArticleUpdateRequest;

public interface ArticleService {

    Long create(ArticleCreateRequest request);

    void update(Long id, ArticleUpdateRequest request);

    void delete(Long id);

    void publish(Long id, ArticlePublishRequest request);

    void offline(Long id);

    ArticleDetailResponse getById(Long id);

    PageResponse<ArticleListItemResponse> list(ArticlePageQuery query);
}
