package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.admin.AccountEntity;

@Mapper
public interface AccountDao extends BaseMapper<AccountEntity> {
}
