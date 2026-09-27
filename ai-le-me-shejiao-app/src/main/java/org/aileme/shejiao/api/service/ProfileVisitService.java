package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.app.ProfileVisitEntity;
import org.aileme.shejiao.domain.vo.ProfileVisitorVo;

import java.util.List;

/**
 * 个人主页访客埋点服务
 */
public interface ProfileVisitService extends IService<ProfileVisitEntity> {

    /**
     * 记录访问（进入主页时调用）
     */
    void recordVisit(Integer visitorUid, Integer targetUid);

    /**
     * 上报停留时长
     */
    void reportDuration(Integer visitorUid, Integer targetUid, Integer seconds);

    /**
     * 查询谁看过我
     */
    List<ProfileVisitorVo> getMyVisitors(Integer uid, Integer page, Integer size);

    /**
     * 访客总数
     */
    Integer getVisitorCount(Integer uid);

    /**
     * 对我感兴趣的人（VIP专属）
     */
    List<ProfileVisitorVo> getInterestedVisitors(Integer uid);

    /**
     * 最近N个访客（主页预览用）
     */
    List<ProfileVisitorVo> getRecentVisitors(Integer uid, Integer limit);
}
