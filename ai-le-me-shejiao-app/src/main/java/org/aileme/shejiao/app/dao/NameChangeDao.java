/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 vx: lwwmmzh
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.dao;

import org.aileme.shejiao.domain.entity.app.NameChangeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
/**
 * 用户名称修改
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-10-07 12:40:50
 */
@Mapper
public interface NameChangeDao extends BaseMapper<NameChangeEntity> {
	
}
