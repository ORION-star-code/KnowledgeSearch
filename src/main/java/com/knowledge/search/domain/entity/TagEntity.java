package com.knowledge.search.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.search.common.model.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_tag")
public class TagEntity extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}
