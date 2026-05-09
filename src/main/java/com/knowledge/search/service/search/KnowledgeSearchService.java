package com.knowledge.search.service.search;

import com.knowledge.search.service.search.dto.SearchRequest;
import com.knowledge.search.service.search.dto.SearchResponse;

public interface KnowledgeSearchService {

    SearchResponse search(SearchRequest request);
}
