package com.knowledge.search.service.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.search.common.api.PageResponse;
import com.knowledge.search.common.enums.SyncFailStatus;
import com.knowledge.search.common.enums.SyncOperation;
import com.knowledge.search.common.exception.BusinessException;
import com.knowledge.search.common.exception.ErrorCode;
import com.knowledge.search.config.AppSyncProperties;
import com.knowledge.search.domain.entity.SyncFailLogEntity;
import com.knowledge.search.mapper.SyncFailLogMapper;
import com.knowledge.search.service.sync.dto.SyncFailLogResponse;
import com.knowledge.search.sync.canal.SyncEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncFailureServiceImpl implements SyncFailureService {

    private final SyncFailLogMapper syncFailLogMapper;
    private final IncrementalSyncServiceImpl incrementalSyncService;
    private final AppSyncProperties appSyncProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(SyncEvent event, Exception exception) {
        SyncFailLogEntity entity = new SyncFailLogEntity();
        entity.setBizType(event.bizType());
        entity.setBizId(event.bizId());
        entity.setPayload(toPayload(event));
        entity.setErrorMsg(exception.getMessage());
        entity.setRetryCount(0);
        entity.setStatus(SyncFailStatus.PENDING);
        entity.setNextRetryTime(LocalDateTime.now().plus(appSyncProperties.retry().initialDelay()));
        syncFailLogMapper.insert(entity);
    }

    @Override
    @Transactional
    public void retry(Long failId) {
        SyncFailLogEntity entity = getRequired(failId);
        retryEntity(entity);
    }

    @Override
    @Transactional
    public void retryPendingFailures() {
        if (!appSyncProperties.retry().enabled()) {
            return;
        }

        syncFailLogMapper.selectList(new LambdaQueryWrapper<SyncFailLogEntity>()
                        .eq(SyncFailLogEntity::getStatus, SyncFailStatus.PENDING)
                        .le(SyncFailLogEntity::getNextRetryTime, LocalDateTime.now())
                        .orderByAsc(SyncFailLogEntity::getId))
                .forEach(this::retryEntity);
    }

    @Override
    public PageResponse<SyncFailLogResponse> listFailures(long pageNum, long pageSize) {
        Page<SyncFailLogEntity> page = syncFailLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SyncFailLogEntity>()
                        .orderByDesc(SyncFailLogEntity::getCreatedAt)
                        .orderByDesc(SyncFailLogEntity::getId));
        return new PageResponse<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toResponse).toList());
    }

    @Override
    public SyncStatsResponse stats() {
        long pending = countByStatus(SyncFailStatus.PENDING);
        long retrying = syncFailLogMapper.selectCount(new LambdaQueryWrapper<SyncFailLogEntity>()
                .eq(SyncFailLogEntity::getStatus, SyncFailStatus.PENDING)
                .gt(SyncFailLogEntity::getRetryCount, 0));
        long failed = countByStatus(SyncFailStatus.FAILED);
        long resolved = countByStatus(SyncFailStatus.SUCCESS);
        return new SyncStatsResponse(pending, retrying, failed, resolved);
    }

    private void retryEntity(SyncFailLogEntity entity) {
        SyncEvent event = parseEvent(entity);
        try {
            incrementalSyncService.handleWithoutFailureRecord(event);
            syncFailLogMapper.update(null, new LambdaUpdateWrapper<SyncFailLogEntity>()
                    .eq(SyncFailLogEntity::getId, entity.getId())
                    .set(SyncFailLogEntity::getStatus, SyncFailStatus.SUCCESS)
                    .set(SyncFailLogEntity::getUpdatedAt, LocalDateTime.now())
                    .set(SyncFailLogEntity::getErrorMsg, null));
        } catch (Exception exception) {
            int nextRetryCount = entity.getRetryCount() + 1;
            boolean exhausted = nextRetryCount >= appSyncProperties.retry().maxAttempts();
            syncFailLogMapper.update(null, new LambdaUpdateWrapper<SyncFailLogEntity>()
                    .eq(SyncFailLogEntity::getId, entity.getId())
                    .set(SyncFailLogEntity::getRetryCount, nextRetryCount)
                    .set(SyncFailLogEntity::getStatus, exhausted ? SyncFailStatus.FAILED : SyncFailStatus.PENDING)
                    .set(SyncFailLogEntity::getErrorMsg, exception.getMessage())
                    .set(SyncFailLogEntity::getNextRetryTime, exhausted ? null : LocalDateTime.now().plus(appSyncProperties.retry().initialDelay()))
                    .set(SyncFailLogEntity::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private SyncFailLogEntity getRequired(Long failId) {
        SyncFailLogEntity entity = syncFailLogMapper.selectById(failId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SYNC_FAIL_NOT_FOUND, "Sync failure record not found");
        }
        return entity;
    }

    private SyncEvent parseEvent(SyncFailLogEntity entity) {
        try {
            return objectMapper.readValue(entity.getPayload(), SyncEvent.class);
        } catch (JsonProcessingException exception) {
            if (entity.getBizType() == null || entity.getBizId() == null) {
                throw new BusinessException(ErrorCode.INVALID_SYNC_PAYLOAD, "Invalid sync payload");
            }
            return new SyncEvent(entity.getBizType(), entity.getBizId(), SyncOperation.REBUILD, entity.getPayload());
        }
    }

    private String toPayload(SyncEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return event.payload();
        }
    }

    private long countByStatus(SyncFailStatus status) {
        return syncFailLogMapper.selectCount(new LambdaQueryWrapper<SyncFailLogEntity>()
                .eq(SyncFailLogEntity::getStatus, status));
    }

    private SyncFailLogResponse toResponse(SyncFailLogEntity entity) {
        return new SyncFailLogResponse(
                entity.getId(),
                entity.getBizType() == null ? null : entity.getBizType().name(),
                entity.getBizId(),
                entity.getPayload(),
                entity.getErrorMsg(),
                entity.getRetryCount(),
                entity.getStatus() == null ? null : entity.getStatus().getValue(),
                entity.getNextRetryTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
