package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.UserLevelEntity;

import java.util.Map;

/**
 * 用户等级服务
 * 
 * @author linfeng
 */
public interface UserLevelService extends IService<UserLevelEntity> {
    
    /**
     * 分页查询用户等级列表
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);
    
    /**
     * 检查并更新用户等级
     * @param uid 用户ID
     */
    void checkUserLevel(Integer uid);
}
