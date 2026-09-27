package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;

import java.util.List;

/**
 * 红娘-用户关联Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface HongniangUserRelationService extends IService<HongniangUserRelationEntity> {

    /**
     * 根据红娘ID查询关联的用户ID列表
     */
    List<Integer> getUserIdsByHongniangId(Integer hongniangId);

    /**
     * 根据用户ID查询关联的红娘ID列表
     */
    List<Integer> getHongniangIdsByUserId(Integer userId);

    /**
     * 批量插入关联关系
     */
    void batchInsert(List<HongniangUserRelationEntity> list);

    /**
     * 自动补全红娘内编号并保存
     */
    void saveRelationWithAutoNo(HongniangUserRelationEntity relation);

    /**
     * 获取指定红娘下一个可用用户编号
     */
    int getNextHongniangUserNo(Integer hongniangId);

    /**
     * 检查红娘是否有权限查看该用户
     */
    boolean hasPermission(Integer hongniangId, Integer userId);

    /**
     * 检查用户是否已关注红娘
     */
    boolean checkFollowed(Integer userId, Integer hongniangId);

    /**
     * 关注红娘
     */
    void follow(Integer userId, Integer hongniangId);

    /**
     * 取消关注
     */
    void unfollow(Integer userId, Integer hongniangId);

    /**
     * 获取用户关注的红娘列表
     */
    List<HongniangInfoEntity> getFollowedHongniangList(Integer userId);
}
