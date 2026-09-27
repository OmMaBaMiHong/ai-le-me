package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.SignResponse;
import org.aileme.shejiao.domain.vo.SignUserResponse;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.SignConfigEntity;
import org.aileme.shejiao.domain.entity.admin.UserSignEntity;

import java.util.List;
import java.util.Map;

/**
 * 签到记录表
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 15:01:03
 */
public interface UserSignService extends IService<UserSignEntity> {

    PageUtils queryPage(Map<String, Object> params);

    SignUserResponse getUserInfo(AppUserEntity user);

    List<SignResponse> getSignList(Integer uid, Integer page, Integer limit);

    int sign(AppUserEntity user);

    List<SignConfigEntity> getConfigList();

    Integer getSignUserCount();

    String consumeIntegral(AppUserEntity user, Integer type);
}

