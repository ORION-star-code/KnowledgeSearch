package com.knowledge.search.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.search.common.enums.ArticleStatus;
import com.knowledge.search.common.model.AuditEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_article")
public class ArticleEntity extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    @TableField("category_id")
    private Long categoryId;

    private String author;

    private ArticleStatus status;

    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("publish_time")
    private LocalDateTime publishTime;
}
