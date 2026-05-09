package com.knowledge.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.search.domain.entity.ArticleTagEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTagEntity> {

    @Delete("DELETE FROM kb_article_tag WHERE article_id = #{articleId}")
    void deleteByArticleId(@Param("articleId") Long articleId);

    @Select({
            "<script>",
            "SELECT id, article_id, tag_id, created_at FROM kb_article_tag",
            "WHERE article_id IN",
            "<foreach item='articleId' collection='articleIds' open='(' separator=',' close=')'>",
            "#{articleId}",
            "</foreach>",
            "</script>"
    })
    List<ArticleTagEntity> selectByArticleIds(@Param("articleIds") Collection<Long> articleIds);

    @Select("SELECT COUNT(1) FROM kb_article_tag WHERE tag_id = #{tagId}")
    long countByTagId(@Param("tagId") Long tagId);

    @Select("SELECT COUNT(1) FROM kb_article_tag WHERE article_id = #{articleId}")
    long countByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT DISTINCT article_id FROM kb_article_tag WHERE tag_id = #{tagId}")
    List<Long> selectArticleIdsByTagId(@Param("tagId") Long tagId);
}
