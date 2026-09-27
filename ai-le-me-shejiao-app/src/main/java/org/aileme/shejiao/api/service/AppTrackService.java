package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.app.AppEventTrackEntity;
import org.aileme.shejiao.domain.param.app.TrackEventDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * App 埋点服务
 */
public interface AppTrackService extends IService<AppEventTrackEntity> {

    /**
     * 批量写入埋点
     */
    Map<String, Object> batchTrack(Integer currentUid, List<TrackEventDTO> events);

    /**
     * 获取画像标签漏斗统计
     */
    List<Map<String, Object>> funnelProfileTag(Date from, Date to);

    /**
     * 提取行为标签
     */
    List<String> collectBehaviorTags(Integer uid, int limit);
}
