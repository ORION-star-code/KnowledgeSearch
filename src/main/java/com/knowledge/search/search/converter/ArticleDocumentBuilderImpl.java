package com.knowledge.search.search.converter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.exception.BusinessException;
import com.knowledge.search.common.exception.ErrorCode;
import com.knowledge.search.domain.entity.ArticleEntity;
import com.knowledge.search.domain.entity.ArticleTagEntity;
import com.knowledge.search.domain.entity.CategoryEntity;
import com.knowledge.search.domain.entity.TagEntity;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.mapper.CategoryMapper;
import com.knowledge.search.mapper.TagMapper;
import com.knowledge.search.search.document.ArticleDocument;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleDocumentBuilderImpl implements ArticleDocumentBuilder {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    @Override
    public ArticleDocument buildByArticleId(Long articleId) {
        ArticleEntity article = articleMapper.selectOne(new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getId, articleId)
                .eq(ArticleEntity::getIsDeleted, false)
                .last("LIMIT 1"));
        return buildByArticles(article == null ? List.of() : List.of(article)).stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ArticleDocument> buildByArticles(Collection<ArticleEntity> articles) {
        if (articles == null || articles.isEmpty()) {
            log.info("article document build skipped because input articles are empty");
            return List.of();
        }

        long start = System.currentTimeMillis();
        List<ArticleEntity> eligibleArticles = articles.stream()
                .filter(article -> article != null && !Boolean.TRUE.equals(article.getIsDeleted()))
                .filter(article -> article.getStatus() == ArticleStatus.PUBLISHED)
                .toList();
        if (eligibleArticles.isEmpty()) {
            log.info("article document build skipped because no eligible published articles, inputSize={}", articles.size());
            return List.of();
        }

        List<Long> articleIds = eligibleArticles.stream()
                .map(ArticleEntity::getId)
                .toList();
        long categoriesStart = System.currentTimeMillis();
        Map<Long, CategoryEntity> categoriesById = categoryMapper.selectBatchIds(eligibleArticles.stream()
                        .map(ArticleEntity::getCategoryId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
        long categoriesMs = System.currentTimeMillis() - categoriesStart;

        long relationsStart = System.currentTimeMillis();
        Map<Long, List<Long>> tagIdsByArticleId = articleTagMapper.selectByArticleIds(articleIds).stream()
                .collect(Collectors.groupingBy(ArticleTagEntity::getArticleId,
                        Collectors.mapping(ArticleTagEntity::getTagId, Collectors.toList())));
        long relationsMs = System.currentTimeMillis() - relationsStart;
        Set<Long> tagIds = tagIdsByArticleId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        long tagsStart = System.currentTimeMillis();
        Map<Long, TagEntity> tagsById = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(TagEntity::getId, Function.identity()));
        long tagsMs = System.currentTimeMillis() - tagsStart;

        List<ArticleDocument> documents = eligibleArticles.stream()
                .map(article -> toDocument(article, categoriesById, tagIdsByArticleId, tagsById))
                .toList();
        log.info("article documents built, inputSize={}, eligibleSize={}, categories={}, articleTagRelations={}, tags={}, categoryQueryMs={}, relationQueryMs={}, tagQueryMs={}, totalMs={}",
                articles.size(),
                eligibleArticles.size(),
                categoriesById.size(),
                tagIdsByArticleId.values().stream().mapToInt(List::size).sum(),
                tagsById.size(),
                categoriesMs,
                relationsMs,
                tagsMs,
                System.currentTimeMillis() - start);
        return documents;
    }

    private ArticleDocument toDocument(ArticleEntity article,
                                       Map<Long, CategoryEntity> categoriesById,
                                       Map<Long, List<Long>> tagIdsByArticleId,
                                       Map<Long, TagEntity> tagsById) {
        CategoryEntity category = categoriesById.get(article.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found");
        }

        List<Long> tagIds = tagIdsByArticleId.getOrDefault(article.getId(), List.of());
        List<String> tagNames = tagIds.stream()
                .map(tagId -> {
                    TagEntity tag = tagsById.get(tagId);
                    if (tag == null) {
                        throw new BusinessException(ErrorCode.TAG_NOT_FOUND, "One or more tags do not exist");
                    }
                    return tag.getName();
                })
                .toList();

        ArticleDocument document = new ArticleDocument();
        document.setId(article.getId());
        document.setTitle(article.getTitle());
        document.setSummary(article.getSummary());
        document.setContent(article.getContent());
        document.setCategoryId(category.getId());
        document.setCategoryName(category.getName());
        document.setTagNames(tagNames);
        document.setAuthor(article.getAuthor());
        document.setStatus(article.getStatus().getValue());
        document.setPublishTime(article.getPublishTime());
        document.setUpdatedAt(article.getUpdatedAt());
        return document;
    }
}
