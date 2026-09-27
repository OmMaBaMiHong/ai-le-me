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

import java.time.Duration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.param.app.LinkListForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.LinkDao;
import org.aileme.shejiao.domain.entity.admin.LinkEntity;
import org.aileme.shejiao.api.service.LinkService;


@DS("master")
@Service("linkService")
public class LinkServiceImpl extends ServiceImpl<LinkDao, LinkEntity> implements LinkService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<LinkEntity> page = this.page(
                new Query<LinkEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<LinkEntity> getPageList(LinkListForm request) {
        if(request.getCateId()==0){
            String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.BANNER_LIST_KEY_SQUARE);
            if(WechatUtil.isEmpty(result)){
                List<LinkEntity> list = this.lambdaQuery()
                        .eq(LinkEntity::getCateId, request.getCateId())
                        .orderByDesc(LinkEntity::getId)
                        .list();
                org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.BANNER_LIST_KEY_SQUARE, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
                return list;
            }
            return JSONObject.parseArray(result, LinkEntity.class);
        }else{
            String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.BANNER_LIST_KEY_MINE);
            if(WechatUtil.isEmpty(result)){
                List<LinkEntity> list = this.lambdaQuery()
                        .eq(LinkEntity::getCateId, request.getCateId())
                        .orderByDesc(LinkEntity::getId)
                        .list();
                org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.BANNER_LIST_KEY_MINE, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
                return list;
            }
            return JSONObject.parseArray(result, LinkEntity.class);
        }

    }

}
