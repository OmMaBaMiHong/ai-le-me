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
import org.aileme.shejiao.admin.utils.WechatUtil;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.CategoryDao;
import org.aileme.shejiao.domain.entity.admin.CategoryEntity;
import org.aileme.shejiao.api.service.CategoryService;


@DS("master")
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveCategory(CategoryEntity category) {
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.TOPIC_CATEGORY_KEY);
        long count = this.lambdaQuery().eq(CategoryEntity::getCateName, category.getCateName()).count();
        if(count!=0){
            throw new LinfengException("分类名不能重复");
        }
        this.save(category);
    }

    @Override
    public List<CategoryEntity> getList() {
        String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.TOPIC_CATEGORY_KEY);
        if(WechatUtil.isEmpty(result)){
            List<CategoryEntity> list = this.lambdaQuery().list();
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.TOPIC_CATEGORY_KEY, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
            return list;
        }
        return JSONObject.parseArray(result, CategoryEntity.class);
    }

}
