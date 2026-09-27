package org.aileme.shejiao.admin.dao;

import org.aileme.shejiao.domain.entity.admin.SensitiveEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * 敏感词库信息表
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-28 13:40:57
 */
@Mapper
public interface SensitiveDao extends BaseMapper<SensitiveEntity> {
	
}
