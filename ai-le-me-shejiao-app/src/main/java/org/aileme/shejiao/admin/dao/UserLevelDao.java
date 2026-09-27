package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.admin.UserLevelEntity;
/**
 * 用户经验值设置
 * 
 * @author pity
 * @email linfengtech002@163.com
 * @date 2023-08-02 15:05:25
 */
@Mapper
public interface UserLevelDao extends BaseMapper<UserLevelEntity> {
	
}
