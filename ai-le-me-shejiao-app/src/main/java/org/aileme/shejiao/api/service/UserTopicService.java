/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.UserTopicEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 21:24:46
 */
public interface UserTopicService extends IService<UserTopicEntity> {

    PageUtils queryPage(Map<String, Object> params);


    Integer findUserTopicService(Integer topicId);


    Boolean isJoin(Integer uid, Integer id);

    List<Integer> getUidByTopicId(Integer id);

    List<Integer> getSomeUidListByTopicId(Integer id);

    List<UserTopicEntity> getTopicIdByUid(Integer uid);
}

