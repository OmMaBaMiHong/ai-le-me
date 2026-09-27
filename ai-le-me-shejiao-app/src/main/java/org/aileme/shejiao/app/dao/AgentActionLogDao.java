package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.app.AgentActionLogEntity;

@Mapper
public interface AgentActionLogDao extends BaseMapper<AgentActionLogEntity> {
}
