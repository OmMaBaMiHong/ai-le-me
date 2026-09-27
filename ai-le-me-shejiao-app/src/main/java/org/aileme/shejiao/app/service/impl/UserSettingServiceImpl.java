package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.UserSettingService;
import org.aileme.shejiao.app.dao.UserSettingDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.UserSettingEntity;
import org.aileme.shejiao.domain.param.app.UpdateUserSettingForm;
import org.aileme.shejiao.domain.vo.AppUserSettingResponse;

import jakarta.annotation.Resource;
import java.util.Map;

@DS("master")
@Service("userSettingService")
public class UserSettingServiceImpl extends ServiceImpl<UserSettingDao, UserSettingEntity> implements UserSettingService {

    @Resource
    private UserSettingDao userSettingDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserSettingEntity> page = this.page(
                new Query<UserSettingEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public AppUserSettingResponse userSetting(Integer uid) {
        UserSettingEntity entity = this.lambdaQuery()
                .eq(UserSettingEntity::getUid, uid)
                .one();
        AppUserSettingResponse response = new AppUserSettingResponse();
        if (entity != null) {
            response.setUid(entity.getUid());
        }
        return response;
    }

    @Override
    public void updateUserSetting(Integer uid, UpdateUserSettingForm param) {
        UserSettingEntity entity = this.lambdaQuery()
                .eq(UserSettingEntity::getUid, uid)
                .one();
        if (entity != null) {
            this.updateById(entity);
        }
    }
}
