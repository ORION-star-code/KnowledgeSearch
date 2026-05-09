package com.knowledge.search.service.search;

import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.query.ArticleSearchQuery;
import com.knowledge.search.search.repository.ArticleIndexRepository;
import com.knowledge.search.search.repository.ArticleSearchHit;
import com.knowledge.search.service.search.dto.SearchHighlight;
import com.knowledge.search.service.search.dto.SearchRequest;
import com.knowledge.search.service.search.dto.SearchResponse;
import com.knowledge.search.service.search.dto.SearchResultItem;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private final ArticleIndexRepository articleIndexRepository;

    @Override
    public SearchResponse search(SearchRequest request) {
        ArticleSearchQuery query = new ArticleSearchQuery(
                request.keyword(),
                request.categoryId(),
                request.tagNames(),
                StringUtils.hasText(request.status()) ? request.status() : ArticleStatus.PUBLISHED.getValue(),
                request.author(),
                request.publishTimeStart(),
                request.publishTimeEnd(),
                request.updatedTimeStart(),
                request.updatedTimeEnd(),
                request.pageNum(),
                request.pageSize(),
                request.sort(),
                Boolean.TRUE.equals(request.highlight()));

        PageResponse<ArticleSearchHit> documentPage = articleIndexRepository.search(query);
        PageResponse<SearchResultItem> page = new PageResponse<>(
                documentPage.pageNum(),
                documentPage.pageSize(),
                documentPage.total(),
                documentPage.records().stream()
                        .map(this::toItem)
                        .toList());
        return new SearchResponse(page);
    }

    private SearchResultItem toItem(ArticleSearchHit hit) {
        ArticleDocument document = hit.document();
        SearchHighlight highlight = toHighlight(hit.highlightFields());
        return new SearchResultItem(
                document.getId(),
                document.getTitle(),
                highlight.title(),
                document.getSummary(),
                highlight.summary(),
                document.getContent(),
                highlight.content(),
                document.getCategoryId(),
                document.getCategoryName(),
                document.getTagNames(),
                document.getAuthor(),
                document.getStatus(),
                document.getPublishTime(),
                document.getUpdatedAt());
    }

    private SearchHighlight toHighlight(Map<String, List<String>> highlightFields) {
        if (highlightFields == null || highlightFields.isEmpty()) {
            return new SearchHighlight(null, null, null);
        }
        return new SearchHighlight(
                firstHighlight(highlightFields, "title"),
                firstHighlight(highlightFields, "summary"),
                firstHighlight(highlightFields, "content"));
    }

    private String firstHighlight(Map<String, List<String>> highlightFields, String fieldName) {
        List<String> fragments = highlightFields.get(fieldName);
        return fragments == null || fragments.isEmpty() ? null : fragments.get(0);
    }
}
