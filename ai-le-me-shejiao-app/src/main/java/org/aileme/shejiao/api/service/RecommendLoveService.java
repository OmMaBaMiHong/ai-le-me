package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.AppReccomentUserResponse;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.vo.SmartMatchRecommendVo;

import java.util.List;

/**
 * <p>
 * 用户推荐设置 服务类
 * </p>
 *
 * @author lww
 * @since 2023-04-29
 */
public interface RecommendLoveService extends IService<RecommendLoveEntity> {


    List<AppReccomentUserResponse> reccomentLoves(AppUserEntity user);

    void getLove(Integer recommendUid, AppUserEntity user);


    void lossLove(Integer recommendUid, AppUserEntity user);

    Result saveLove(RecommendLoveEntity entity, AppUserEntity user);

    void sendNots(Integer recommendUid,String nots, AppUserEntity user);

    /**
     * 同城推荐列表
     * @param user 当前用户
     * @param city 前端传入的城市名（可为null，自动取用户定位城市）
     * @param page 页码
     * @param size 每页数量
     */
    List<AppReccomentUserResponse> sameCityLoves(AppUserEntity user, String city, Integer page, Integer size);

    SmartMatchRecommendVo smartMatchLoves(AppUserEntity user, Integer page, Integer size);

    void preGenerateSmartMatch(AppUserEntity user);

    String replenishDailySmartMatches(Integer batchSize);
}
