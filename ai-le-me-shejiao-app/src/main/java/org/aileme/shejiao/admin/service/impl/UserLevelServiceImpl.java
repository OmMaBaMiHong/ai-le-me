package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.Duration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserLevelEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.UserLevelService;
import org.aileme.shejiao.admin.dao.UserLevelDao;
import org.aileme.shejiao.admin.utils.WechatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


@DS("master")
@Service("userLevelService")
public class UserLevelServiceImpl extends ServiceImpl<UserLevelDao, UserLevelEntity> implements UserLevelService {


    @Autowired
    private AppUserService appUserService;
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<UserLevelEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(UserLevelEntity::getLevelId);
        IPage<UserLevelEntity> page = this.page(
                new Query<UserLevelEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    public Integer updateUserLevel(AppUserEntity user) {
        List<UserLevelEntity> list = this.getList();
        if (list.size() == 0) {
            return 0;
        }
        Integer integral = user.getIntegral();
        Integer userLevel = list.get(list.size() - 1).getLevelId();
        for (int i = 0; i < list.size(); i++) {
            if (integral < list.get(i).getMaxNum()) {
                userLevel = list.get(i).getLevelId();
                break;
            }
        }
        if (!user.getLevel().equals(userLevel)) {
            boolean update = appUserService.lambdaUpdate()
                    .set(AppUserEntity::getLevel, userLevel)
                    .eq(AppUserEntity::getUid, user.getUid())
                    .update();
            if (!update) {
                throw new LinfengException("用户等级信息更新失败");
            }
            org.aileme.common.redis.utils.RedisUtils.deleteObject(RedisKeys.getUserCacheKey(user.getUid()));
        }
        return userLevel;
    }

    @Override
    public void checkUserLevel(Integer uid) {
        AppUserEntity user = appUserService.getById(uid);
        if (user == null) {
            return;
        }
        List<UserLevelEntity> list = this.getList();
        if (list.size() == 0) {
            return;
        }
        Integer integral = user.getIntegral();
        Integer userLevel = list.get(list.size() - 1).getLevelId();
        for (int i = 0; i < list.size(); i++) {
            if (integral < list.get(i).getMaxNum()) {
                userLevel = list.get(i).getLevelId();
                break;
            }
        }
        if (!user.getLevel().equals(userLevel)) {
            boolean update = appUserService.lambdaUpdate()
                    .set(AppUserEntity::getLevel, userLevel)
                    .eq(AppUserEntity::getUid, user.getUid())
                    .update();
            if (!update) {
                throw new LinfengException("用户等级信息更新失败");
            }
            org.aileme.common.redis.utils.RedisUtils.deleteObject(RedisKeys.getUserCacheKey(user.getUid()));
        }
    }


    public List<UserLevelEntity> getList() {
        String result = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.USER_LEVEL_KEY);
        if(WechatUtil.isEmpty(result)){
            List<UserLevelEntity> list = this.lambdaQuery()
                    .orderByAsc(UserLevelEntity::getLevelId)
                    .list();
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.USER_LEVEL_KEY, JSON.toJSON(list).toString(), Duration.ofSeconds(60 * 60 * 24));
            return list;
        }
        return JSONObject.parseArray(result, UserLevelEntity.class);
    }

}
