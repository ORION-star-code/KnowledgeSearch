package com.knowledge.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.search.domain.entity.ArticleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleMapper extends BaseMapper<ArticleEntity> {

    @Select("SELECT COUNT(1) FROM kb_article WHERE category_id = #{categoryId} AND is_deleted = FALSE")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT id FROM kb_article WHERE category_id = #{categoryId} AND is_deleted = FALSE")
    List<Long> selectIdsByCategoryId(@Param("categoryId") Long categoryId);
}
