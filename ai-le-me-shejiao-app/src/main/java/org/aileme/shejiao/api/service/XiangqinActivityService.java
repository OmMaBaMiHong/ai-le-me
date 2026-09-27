package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;

import java.util.List;
import java.util.Map;

/**
 * 相亲局活动Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface XiangqinActivityService extends IService<XiangqinActivityEntity> {

    /**
     * 分页查询活动列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 创建相亲局活动
     */
    void saveActivity(XiangqinActivityEntity activity);

    /**
     * 更新相亲局活动
     */
    void updateActivity(XiangqinActivityEntity activity);

    /**
     * 根据红娘ID查询活动列表
     */
    List<XiangqinActivityEntity> getByHongniangId(Integer hongniangId);

    /**
     * 增加浏览次数
     */
    void increaseViewCount(Integer activityId);

    /**
     * 更新报名人数
     */
    void updateEnrollCount(Integer activityId);

    /**
     * 获取App端活动列表
     */
    PageUtils getAppActivityList(Map<String, Object> params);
}
