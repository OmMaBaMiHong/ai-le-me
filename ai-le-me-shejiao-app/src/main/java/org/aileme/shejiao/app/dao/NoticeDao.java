package org.aileme.shejiao.app.dao;

import org.aileme.shejiao.domain.entity.app.NoticeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * IM通知表
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 15:04:19
 */
@Mapper
public interface NoticeDao extends BaseMapper<NoticeEntity> {
	
}
