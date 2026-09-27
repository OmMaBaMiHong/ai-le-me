package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.app.UserImpressionTagEntity;

/**
 * 用户印象标签记录 DAO
 */
@Mapper
public interface UserImpressionTagDao extends BaseMapper<UserImpressionTagEntity> {
}
