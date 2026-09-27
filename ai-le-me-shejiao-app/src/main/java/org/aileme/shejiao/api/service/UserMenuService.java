package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.UserMenuEntity;

import java.util.List;
import java.util.Map;

/**
 * 用户菜单
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-07-22 09:33:30
 */
public interface UserMenuService extends IService<UserMenuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<UserMenuEntity> menuList();
}

