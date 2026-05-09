package com.knowledge.search.search.repository;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.query.ArticleSearchQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
@Profile("!test")
@RequiredArgsConstructor
public class ElasticsearchArticleIndexRepository implements ArticleIndexRepository {

    private final ElasticsearchOperations operations;

    @Override
    public void save(ArticleDocument document) {
        if (document == null || document.getId() == null) {
            return;
        }
        log.info("es save single started, articleId={}", document.getId());
        operations.save(document);
        log.info("es save single completed, articleId={}", document.getId());
    }

    @Override
    public void saveAll(Collection<ArticleDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        log.info("es save batch started, size={}", documents.size());
        operations.save(documents);
        log.info("es save batch completed, size={}, elapsedMs={}", documents.size(), System.currentTimeMillis() - start);
    }

    @Override
    public void deleteById(Long articleId) {
        if (articleId == null) {
            return;
        }
        log.info("es delete started, articleId={}", articleId);
        operations.delete(String.valueOf(articleId), ArticleDocument.class);
        log.info("es delete completed, articleId={}", articleId);
    }

    @Override
    public PageResponse<ArticleSearchHit> search(ArticleSearchQuery query) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(buildQuery(query))
                .withPageable(PageRequest.of(Math.max((int) query.pageNum() - 1, 0), (int) query.pageSize()))
                .withSort(buildSorts(query.keyword(), query.sort()));
        if (query.highlight() && StringUtils.hasText(query.keyword())) {
            queryBuilder.withHighlightQuery(buildHighlightQuery());
        }

        SearchHits<ArticleDocument> hits = operations.search(queryBuilder.build(), ArticleDocument.class);
        List<ArticleSearchHit> records = hits.stream()
                .map(hit -> new ArticleSearchHit(hit.getContent(), hit.getHighlightFields()))
                .toList();
        return new PageResponse<>(query.pageNum(), query.pageSize(), hits.getTotalHits(), records);
    }

    @Override
    public void deleteAll() {
        IndexOperations indexOperations = operations.indexOps(ArticleDocument.class);
        long start = System.currentTimeMillis();
        if (indexOperations.exists()) {
            log.info("es index reset started, action=delete-index");
            boolean deleted = indexOperations.delete();
            log.info("es index delete completed, deleted={}", deleted);
        } else {
            log.info("es index reset skipped delete because index does not exist");
        }
        boolean created = indexOperations.create();
        boolean mappingCreated = indexOperations.putMapping();
        log.info("es index reset completed, created={}, mappingCreated={}, elapsedMs={}",
                created, mappingCreated, System.currentTimeMillis() - start);
    }

    private Query buildQuery(ArticleSearchQuery query) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        if (StringUtils.hasText(query.keyword())) {
            mustQueries.add(QueryBuilders.multiMatch()
                    .query(query.keyword())
                    .fields("title^3", "summary^2", "content")
                    .type(TextQueryType.BestFields)
                    .build()
                    ._toQuery());
        }

        if (query.categoryId() != null) {
            filterQueries.add(QueryBuilders.term()
                    .field("categoryId")
                    .value(query.categoryId())
                    .build()
                    ._toQuery());
        }

        if (StringUtils.hasText(query.status())) {
            filterQueries.add(QueryBuilders.term()
                    .field("status")
                    .value(query.status())
                    .build()
                    ._toQuery());
        }

        if (StringUtils.hasText(query.author())) {
            filterQueries.add(QueryBuilders.term()
                    .field("author")
                    .value(query.author())
                    .build()
                    ._toQuery());
        }

        if (!CollectionUtils.isEmpty(query.tagNames())) {
            query.tagNames().stream()
                    .filter(StringUtils::hasText)
                    .map(tagName -> QueryBuilders.term().field("tagNames").value(tagName).build()._toQuery())
                    .forEach(filterQueries::add);
        }

        addRangeFilter(filterQueries, "publishTime", query.publishTimeStart(), query.publishTimeEnd());
        addRangeFilter(filterQueries, "updatedAt", query.updatedTimeStart(), query.updatedTimeEnd());

        if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
            return QueryBuilders.matchAll().build()._toQuery();
        }

        return QueryBuilders.bool()
                .must(mustQueries)
                .filter(filterQueries)
                .build()
                ._toQuery();
    }

    private void addRangeFilter(List<Query> filterQueries, String field, LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return;
        }
        filterQueries.add(QueryBuilders.range()
                .field(field)
                .from(start == null ? null : start.toString())
                .to(end == null ? null : end.toString())
                .build()
                ._toQuery());
    }

    private List<SortOptions> buildSorts(String keyword, String sort) {
        List<SortOptions> sorts = new ArrayList<>();
        if (!"latest".equalsIgnoreCase(sort) && StringUtils.hasText(keyword)) {
            sorts.add(SortOptions.of(option -> option.score(score -> score.order(SortOrder.Desc))));
        }
        sorts.add(SortOptions.of(option -> option.field(field -> field.field("publishTime").order(SortOrder.Desc))));
        sorts.add(SortOptions.of(option -> option.field(field -> field.field("updatedAt").order(SortOrder.Desc))));
        sorts.add(SortOptions.of(option -> option.field(field -> field.field("id").order(SortOrder.Desc))));
        return sorts;
    }

    private HighlightQuery buildHighlightQuery() {
        Highlight highlight = new Highlight(
                HighlightParameters.builder()
                        .withPreTags("<em>")
                        .withPostTags("</em>")
                        .build(),
                List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content")));
        return new HighlightQuery(highlight, ArticleDocument.class);
    }
}
