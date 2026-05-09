package com.knowledge.search.search.repository;

import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.query.ArticleSearchQuery;
import java.util.Collection;

public interface ArticleIndexRepository {

    void save(ArticleDocument document);

    void saveAll(Collection<ArticleDocument> documents);

    void deleteById(Long articleId);

    PageResponse<ArticleSearchHit> search(ArticleSearchQuery query);

    void deleteAll();
}
