package com.knowledge.search.service.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.exception.BusinessException;
import com.knowledge.search.common.exception.ErrorCode;
import com.knowledge.search.domain.entity.ArticleEntity;
import com.knowledge.search.domain.entity.ArticleTagEntity;
import com.knowledge.search.domain.entity.CategoryEntity;
import com.knowledge.search.domain.entity.TagEntity;
import com.knowledge.search.mapper.ArticleMapper;
import com.knowledge.search.mapper.ArticleTagMapper;
import com.knowledge.search.service.article.dto.ArticleCreateRequest;
import com.knowledge.search.service.article.dto.ArticleDetailResponse;
import com.knowledge.search.service.article.dto.ArticleListItemResponse;
import com.knowledge.search.service.article.dto.ArticlePageQuery;
import com.knowledge.search.service.article.dto.ArticlePublishRequest;
import com.knowledge.search.service.article.dto.ArticleUpdateRequest;
import com.knowledge.search.service.article.dto.CategorySimpleResponse;
import com.knowledge.search.service.article.dto.TagSimpleResponse;
import com.knowledge.search.service.category.CategoryService;
import com.knowledge.search.service.tag.TagService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryService categoryService;
    private final TagService tagService;

    @Override
    @Transactional
    public Long create(ArticleCreateRequest request) {
        CategoryEntity category = categoryService.getRequired(request.categoryId());
        List<TagEntity> tags = tagService.getRequiredByIds(normalizeTagIds(request.tagIds()));

        ArticleEntity entity = new ArticleEntity();
        applyWritableFields(entity, request.title(), request.summary(), request.content(), request.categoryId(), request.author());
        entity.setStatus(ArticleStatus.DRAFT);
        entity.setIsDeleted(Boolean.FALSE);
        articleMapper.insert(entity);

        replaceTags(entity.getId(), tags);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ArticleUpdateRequest request) {
        ArticleEntity entity = getRequiredArticle(id);
        categoryService.getRequired(request.categoryId());
        List<TagEntity> tags = tagService.getRequiredByIds(normalizeTagIds(request.tagIds()));

        applyWritableFields(entity, request.title(), request.summary(), request.content(), request.categoryId(), request.author());
        articleMapper.updateById(entity);
        replaceTags(id, tags);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequiredArticle(id);
        articleTagMapper.deleteByArticleId(id);
        articleMapper.update(null, new LambdaUpdateWrapper<ArticleEntity>()
                .eq(ArticleEntity::getId, id)
                .eq(ArticleEntity::getIsDeleted, false)
                .set(ArticleEntity::getIsDeleted, true));
    }

    @Override
    @Transactional
    public void publish(Long id, ArticlePublishRequest request) {
        getRequiredArticle(id);
        articleMapper.update(null, new LambdaUpdateWrapper<ArticleEntity>()
                .eq(ArticleEntity::getId, id)
                .eq(ArticleEntity::getIsDeleted, false)
                .set(ArticleEntity::getStatus, ArticleStatus.PUBLISHED)
                .set(ArticleEntity::getPublishTime, request.publishTime()));
    }

    @Override
    @Transactional
    public void offline(Long id) {
        getRequiredArticle(id);
        articleMapper.update(null, new LambdaUpdateWrapper<ArticleEntity>()
                .eq(ArticleEntity::getId, id)
                .eq(ArticleEntity::getIsDeleted, false)
                .set(ArticleEntity::getStatus, ArticleStatus.OFFLINE));
    }

    @Override
    public ArticleDetailResponse getById(Long id) {
        ArticleEntity article = getRequiredArticle(id);
        CategoryEntity category = categoryService.getRequired(article.getCategoryId());
        List<TagEntity> tags = findTagsByArticleIds(List.of(id)).getOrDefault(id, List.of());
        return toDetailResponse(article, category, tags);
    }

    @Override
    public PageResponse<ArticleListItemResponse> list(ArticlePageQuery query) {
        LambdaQueryWrapper<ArticleEntity> wrapper = new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getIsDeleted, false)
                .orderByDesc(ArticleEntity::getUpdatedAt)
                .orderByDesc(ArticleEntity::getId);

        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(ArticleEntity::getTitle, query.keyword())
                    .or()
                    .like(ArticleEntity::getSummary, query.keyword())
                    .or()
                    .like(ArticleEntity::getContent, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(ArticleEntity::getStatus, parseStatus(query.status()));
        }
        if (query.categoryId() != null) {
            wrapper.eq(ArticleEntity::getCategoryId, query.categoryId());
        }

        Page<ArticleEntity> page = articleMapper.selectPage(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        List<ArticleEntity> records = page.getRecords();
        Map<Long, CategoryEntity> categoryMap = records.isEmpty()
                ? Map.of()
                : records.stream()
                        .map(ArticleEntity::getCategoryId)
                        .distinct()
                        .map(categoryService::getRequired)
                        .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
        Map<Long, List<TagEntity>> tagsByArticleId = findTagsByArticleIds(records.stream().map(ArticleEntity::getId).toList());

        List<ArticleListItemResponse> items = records.stream()
                .map(article -> toListItemResponse(
                        article,
                        categoryMap.get(article.getCategoryId()),
                        tagsByArticleId.getOrDefault(article.getId(), List.of())))
                .toList();
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), items);
    }

    private void applyWritableFields(ArticleEntity entity, String title, String summary, String content, Long categoryId, String author) {
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setContent(content);
        entity.setCategoryId(categoryId);
        entity.setAuthor(author);
    }

    private ArticleEntity getRequiredArticle(Long id) {
        ArticleEntity entity = articleMapper.selectOne(new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getId, id)
                .eq(ArticleEntity::getIsDeleted, false)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, "Article not found");
        }
        return entity;
    }

    private Set<Long> normalizeTagIds(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Set.of();
        }
        return tagIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void replaceTags(Long articleId, List<TagEntity> tags) {
        articleTagMapper.deleteByArticleId(articleId);
        for (TagEntity tag : tags) {
            ArticleTagEntity relation = new ArticleTagEntity();
            relation.setArticleId(articleId);
            relation.setTagId(tag.getId());
            articleTagMapper.insert(relation);
        }
    }

    private Map<Long, List<TagEntity>> findTagsByArticleIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }

        List<ArticleTagEntity> relations = articleTagMapper.selectByArticleIds(articleIds);
        if (relations.isEmpty()) {
            return Map.of();
        }

        List<TagEntity> tags = tagService.getRequiredByIds(relations.stream().map(ArticleTagEntity::getTagId).toList());
        Map<Long, TagEntity> tagMap = tags.stream().collect(Collectors.toMap(TagEntity::getId, Function.identity()));

        return relations.stream().collect(Collectors.groupingBy(
                ArticleTagEntity::getArticleId,
                Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                        .sorted(Comparator.comparing(ArticleTagEntity::getId))
                        .map(relation -> tagMap.get(relation.getTagId()))
                        .filter(java.util.Objects::nonNull)
                        .toList())
        ));
    }

    private ArticleDetailResponse toDetailResponse(ArticleEntity article, CategoryEntity category, List<TagEntity> tags) {
        return new ArticleDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getContent(),
                article.getAuthor(),
                article.getStatus().getValue(),
                article.getPublishTime(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                toCategorySimple(category),
                toTagSimple(tags));
    }

    private ArticleListItemResponse toListItemResponse(ArticleEntity article, CategoryEntity category, List<TagEntity> tags) {
        return new ArticleListItemResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getAuthor(),
                article.getStatus().getValue(),
                article.getPublishTime(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                toCategorySimple(category),
                toTagSimple(tags));
    }

    private CategorySimpleResponse toCategorySimple(CategoryEntity category) {
        return new CategorySimpleResponse(category.getId(), category.getName());
    }

    private List<TagSimpleResponse> toTagSimple(List<TagEntity> tags) {
        return tags.stream()
                .map(tag -> new TagSimpleResponse(tag.getId(), tag.getName()))
                .toList();
    }

    private ArticleStatus parseStatus(String status) {
        return switch (status.toLowerCase()) {
            case "draft" -> ArticleStatus.DRAFT;
            case "published" -> ArticleStatus.PUBLISHED;
            case "offline" -> ArticleStatus.OFFLINE;
            default -> throw new BusinessException("INVALID_STATUS", "Unsupported article status");
        };
    }
}
