package com.knowledge.search.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.search.common.enums.BizType;
import com.knowledge.search.common.enums.SyncFailStatus;
import com.knowledge.search.common.model.AuditEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sync_fail_log")
public class SyncFailLogEntity extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    private BizType bizType;

    @TableField("biz_id")
    private Long bizId;

    private String payload;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("retry_count")
    private Integer retryCount;

    private SyncFailStatus status;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;
}
