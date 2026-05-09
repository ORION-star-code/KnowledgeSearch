package com.knowledge.search.search.repository;

import com.knowledge.search.search.document.ArticleDocument;
import java.util.List;
import java.util.Map;

public record ArticleSearchHit(
        ArticleDocument document,
        Map<String, List<String>> highlightFields
) {
}
