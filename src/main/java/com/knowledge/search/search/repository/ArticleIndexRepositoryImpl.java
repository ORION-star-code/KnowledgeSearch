package com.knowledge.search.search.repository;

import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.search.document.ArticleDocument;
import com.knowledge.search.search.query.ArticleSearchQuery;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@Profile("test")
public class ArticleIndexRepositoryImpl implements ArticleIndexRepository {

    private final Map<Long, ArticleDocument> store = new ConcurrentHashMap<>();

    @Override
    public void save(ArticleDocument document) {
        if (document == null || document.getId() == null) {
            return;
        }
        store.put(document.getId(), document);
    }

    @Override
    public void saveAll(Collection<ArticleDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        documents.forEach(this::save);
    }

    @Override
    public void deleteById(Long articleId) {
        store.remove(articleId);
    }

    @Override
    public PageResponse<ArticleSearchHit> search(ArticleSearchQuery query) {
        List<ArticleDocument> matched = store.values().stream()
                .filter(document -> matches(document, query))
                .sorted(buildComparator(query.keyword(), query.sort()))
                .toList();
        List<ArticleSearchHit> paged = matched.stream()
                .skip((query.pageNum() - 1) * query.pageSize())
                .limit(query.pageSize())
                .map(document -> new ArticleSearchHit(document, buildHighlightFields(document, query)))
                .toList();
        return new PageResponse<>(query.pageNum(), query.pageSize(), matched.size(), paged);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    private boolean matches(ArticleDocument document, ArticleSearchQuery query) {
        if (query.categoryId() != null && !query.categoryId().equals(document.getCategoryId())) {
            return false;
        }
        if (StringUtils.hasText(query.status()) && !query.status().equalsIgnoreCase(document.getStatus())) {
            return false;
        }
        if (StringUtils.hasText(query.author()) && !query.author().equalsIgnoreCase(document.getAuthor())) {
            return false;
        }
        if (!withinRange(document.getPublishTime(), query.publishTimeStart(), query.publishTimeEnd())) {
            return false;
        }
        if (!withinRange(document.getUpdatedAt(), query.updatedTimeStart(), query.updatedTimeEnd())) {
            return false;
        }
        if (query.tagNames() != null && !query.tagNames().isEmpty()) {
            List<String> docTags = document.getTagNames() == null ? List.of() : document.getTagNames();
            boolean allMatched = query.tagNames().stream().allMatch(tag -> docTags.stream().anyMatch(tag::equalsIgnoreCase));
            if (!allMatched) {
                return false;
            }
        }
        if (!StringUtils.hasText(query.keyword())) {
            return true;
        }
        String keyword = query.keyword().toLowerCase();
        return contains(document.getTitle(), keyword)
                || contains(document.getSummary(), keyword)
                || contains(document.getContent(), keyword);
    }

    private Comparator<ArticleDocument> buildComparator(String keyword, String sort) {
        if ("latest".equalsIgnoreCase(sort) || !StringUtils.hasText(keyword)) {
            return Comparator.comparing(ArticleDocument::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ArticleDocument::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ArticleDocument::getId, Comparator.reverseOrder());
        }

        return Comparator.<ArticleDocument>comparingInt(document -> score(document, keyword))
                .reversed()
                .thenComparing(ArticleDocument::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ArticleDocument::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ArticleDocument::getId, Comparator.reverseOrder());
    }

    private int score(ArticleDocument document, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return 0;
        }
        String normalized = keyword.toLowerCase();
        return exactOrPrefixBonus(document.getTitle(), normalized)
                + weightedOccurrences(document.getTitle(), normalized, 3)
                + weightedOccurrences(document.getSummary(), normalized, 2)
                + weightedOccurrences(document.getContent(), normalized, 1);
    }

    private int exactOrPrefixBonus(String title, String keyword) {
        if (!StringUtils.hasText(title)) {
            return 0;
        }
        String normalizedTitle = title.toLowerCase();
        if (normalizedTitle.equals(keyword)) {
            return 100;
        }
        if (normalizedTitle.startsWith(keyword)) {
            return 20;
        }
        return 0;
    }

    private int weightedOccurrences(String source, String keyword, int weight) {
        if (!contains(source, keyword)) {
            return 0;
        }
        return count(source.toLowerCase(), keyword) * weight;
    }

    private boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase().contains(keyword);
    }

    private int count(String source, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(keyword, index)) >= 0) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private boolean withinRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return start == null && end == null;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        if (end != null && value.isAfter(end)) {
            return false;
        }
        return true;
    }

    private Map<String, List<String>> buildHighlightFields(ArticleDocument document, ArticleSearchQuery query) {
        if (!query.highlight() || !StringUtils.hasText(query.keyword())) {
            return Map.of();
        }
        Map<String, List<String>> highlightFields = new LinkedHashMap<>();
        addHighlight(highlightFields, "title", document.getTitle(), query.keyword());
        addHighlight(highlightFields, "summary", document.getSummary(), query.keyword());
        addHighlight(highlightFields, "content", document.getContent(), query.keyword());
        return highlightFields;
    }

    private void addHighlight(Map<String, List<String>> highlightFields, String field, String source, String keyword) {
        String highlighted = highlight(source, keyword);
        if (highlighted != null) {
            highlightFields.put(field, List.of(highlighted));
        }
    }

    private String highlight(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return null;
        }
        String lowerSource = source.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerSource.indexOf(lowerKeyword);
        if (index < 0) {
            return null;
        }
        return source.substring(0, index)
                + "<em>"
                + source.substring(index, index + keyword.length())
                + "</em>"
                + source.substring(index + keyword.length());
    }
}
