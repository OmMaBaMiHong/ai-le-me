package org.aileme.shejiao.app.dao;

import org.aileme.shejiao.domain.entity.app.UserSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * 用户隐私设置表
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-24 15:17:15
 */
@Mapper
public interface UserSettingDao extends BaseMapper<UserSettingEntity> {
	
}
