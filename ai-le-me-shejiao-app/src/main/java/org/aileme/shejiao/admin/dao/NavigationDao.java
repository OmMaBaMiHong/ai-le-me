package org.aileme.shejiao.admin.dao;

import org.aileme.shejiao.domain.entity.admin.NavigationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * 导航栏模块
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-04 22:14:59
 */
@Mapper
public interface NavigationDao extends BaseMapper<NavigationEntity> {
	
}
