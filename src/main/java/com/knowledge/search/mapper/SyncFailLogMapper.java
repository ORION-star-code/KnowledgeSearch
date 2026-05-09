package com.knowledge.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.search.domain.entity.SyncFailLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncFailLogMapper extends BaseMapper<SyncFailLogEntity> {
}
