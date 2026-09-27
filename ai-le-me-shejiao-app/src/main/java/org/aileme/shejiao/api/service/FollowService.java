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

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.FollowBatchResponse;
import org.aileme.shejiao.domain.vo.HotUserResponse;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.FollowEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 14:38:31
 */
public interface FollowService extends IService<FollowEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Integer isFollow(Integer uid,Integer followUid);

    boolean isFollowOrNot(Integer uid,Integer followUid);

    List<FollowBatchResponse> findFollowBatch(List<Integer> list,Integer uid);

    List<Integer> getFollowUids(AppUserEntity user);

    Integer getFollowCount(Integer uid);

    Integer getFans(Integer uid);

    List<Integer> getFansList(Integer uid);

    List<Integer> queryFollows(Integer uid, Integer recommendUid);

    List<HotUserResponse> getHotUserList(DateTime dateTime);
}

