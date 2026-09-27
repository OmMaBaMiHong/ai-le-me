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
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.admin.utils.WechatUtil;import org.aileme.shejiao.domain.entity.admin.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.SensitiveDao;
import org.aileme.shejiao.domain.entity.admin.SensitiveEntity;
import org.aileme.shejiao.api.service.SensitiveService;


@DS("master")
@Service("sensitiveService")
public class SensitiveServiceImpl extends ServiceImpl<SensitiveDao, SensitiveEntity> implements SensitiveService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SensitiveEntity> page = this.page(
                new Query<SensitiveEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public Boolean checkContent(String content) {

        List<SensitiveEntity> list = getSensitiveList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null || list.get(i).getSensitiveWord().isEmpty()) {
                return false;
            }
            if (list.get(i).getState().equals(Constant.SENSITIVE_CLOSE)) {
                return false;
            }
            //校验是否存在敏感词
            String[] split = list.get(i).getSensitiveWord().split(",");
            for (String word : split) {
                if (content.contains(word)) {
                    //如果存在敏感词
                    if (Constant.DEAL_BANNER.equals(list.get(i).getHandleMeasures())) {
                        throw new LinfengException("内容违规禁止发布");
                    } else {
                        //设置评论敏感词状态为需审核
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void checkPostContent(String content) {
        List<SensitiveEntity> list = getSensitiveList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null || list.get(i).getSensitiveWord().isEmpty()) {
                return;
            }
            if (list.get(i).getState().equals(Constant.SENSITIVE_CLOSE)) {
                return;
            }
            //校验是否存在敏感词
            String[] split = list.get(i).getSensitiveWord().split(",");
            for (String word : split) {
                if (content.contains(word)) {
                    //如果存在敏感词 直接打回
                    throw new LinfengException("内容含敏感词禁止发布");
                }
            }
        }

    }

    /**
     * 缓存获取敏感词列表
     * @return
     */
    private List<SensitiveEntity> getSensitiveList(){
        String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.SENSETIVE_LIST_KEY);
        if(WechatUtil.isEmpty(result)){
            List<SensitiveEntity> list = this.lambdaQuery().list();
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.SENSETIVE_LIST_KEY, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
            return list;
        }
        return JSONObject.parseArray(result, SensitiveEntity.class);
    }

}
