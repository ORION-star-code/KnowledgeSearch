package com.knowledge.search.search.converter;

import com.knowledge.search.domain.entity.ArticleEntity;
import com.knowledge.search.search.document.ArticleDocument;
import java.util.Collection;
import java.util.List;

public interface ArticleDocumentBuilder {

    ArticleDocument buildByArticleId(Long articleId);

    List<ArticleDocument> buildByArticles(Collection<ArticleEntity> articles);
}
