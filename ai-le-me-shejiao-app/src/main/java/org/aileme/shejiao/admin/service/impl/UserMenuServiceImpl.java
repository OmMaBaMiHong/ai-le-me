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
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.aileme.common.redis.utils.RedisUtils;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.UserMenuDao;
import org.aileme.shejiao.domain.entity.admin.UserMenuEntity;
import org.aileme.shejiao.api.service.UserMenuService;


@DS("master")
@Slf4j
@Service("userMenuService")
public class UserMenuServiceImpl extends ServiceImpl<UserMenuDao, UserMenuEntity> implements UserMenuService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        normalizePageParams(params);
        QueryWrapper<UserMenuEntity> queryWrapper=new QueryWrapper<>();
        Object keywordObj = params.get("key");
        if (keywordObj != null) {
            String keyword = String.valueOf(keywordObj).trim();
            if (!keyword.isEmpty()) {
                queryWrapper.lambda().and(wrapper ->
                        wrapper.like(UserMenuEntity::getName, keyword)
                                .or()
                                .like(UserMenuEntity::getUrl, keyword)
                );
            }
        }
        queryWrapper.lambda().orderByDesc(UserMenuEntity::getSort);
        IPage<UserMenuEntity> page = this.page(
                new Query<UserMenuEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    private void normalizePageParams(Map<String, Object> params) {
        if (params.get(Constant.PAGE) == null && params.get("pageNum") != null) {
            params.put(Constant.PAGE, String.valueOf(params.get("pageNum")));
        }
        if (params.get(Constant.LIMIT) == null && params.get("pageSize") != null) {
            params.put(Constant.LIMIT, String.valueOf(params.get("pageSize")));
        }
    }

    @Override
    public List<UserMenuEntity> menuList() {
        String result = RedisUtils.getCacheObject(ConfigConstant.USER_MENU_CONFIG_KEY);
        if (!WechatUtil.isEmpty(result)) {
            List<UserMenuEntity> cachedList = JSONObject.parseArray(result, UserMenuEntity.class);
            if (cachedList != null && !cachedList.isEmpty()) {
                return cachedList;
            }
            log.warn("检测到 user_menu 空缓存，忽略 Redis 并回源数据库重建缓存，cacheKey={}", ConfigConstant.USER_MENU_CONFIG_KEY);
            RedisUtils.deleteObject(ConfigConstant.USER_MENU_CONFIG_KEY);
        }
        List<UserMenuEntity> list = this.lambdaQuery()
                .eq(UserMenuEntity::getStatus, 0)
                .orderByDesc(UserMenuEntity::getSort)
                .list();
        if (list != null && !list.isEmpty()) {
            RedisUtils.setCacheObject(ConfigConstant.USER_MENU_CONFIG_KEY, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
        } else {
            log.warn("user_menu 可见菜单查询结果为空，本次不写入长缓存，避免空结果污染前端入口展示");
        }
        return list;
    }

}
